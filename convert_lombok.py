#!/usr/bin/env python3
"""
Script to convert Lombok annotations to explicit Java code.
This handles @Data, @Builder, @NoArgsConstructor, @AllArgsConstructor, @Slf4j, @Getter, @Setter
"""

import os
import re
import sys
from pathlib import Path

def extract_class_name(content):
    """Extract the public class name from Java file content."""
    match = re.search(r'public\s+(?:class|enum|interface)\s+(\w+)', content)
    if match:
        return match.group(1)
    return None

def extract_package(content):
    """Extract package declaration from Java file content."""
    match = re.search(r'package\s+([\w.]+);', content)
    if match:
        return match.group(1)
    return None

def extract_fields(content):
    """Extract field declarations from Java class."""
    fields = []
    # Pattern to match field declarations (including annotations)
    field_pattern = r'((?:\s*@\w+(?:\([^)]*\))?\s*)*)\s*(private\s+(?:final\s+)?(\w+(?:<[^>]+>)?)\s+(\w+)\s*;)'

    for match in re.finditer(field_pattern, content):
        annotations = match.group(1).strip()
        modifiers = match.group(2)
        field_type = match.group(3)
        field_name = match.group(4)
        fields.append({
            'annotations': annotations,
            'modifiers': modifiers,
            'type': field_type,
            'name': field_name
        })

    return fields

def has_lombok_annotation(content, annotation):
    """Check if content has a specific Lombok annotation."""
    pattern = r'@' + annotation + r'(?:\([^)]*\))?\s*\n'
    return bool(re.search(pattern, content))

def remove_lombok_imports(content):
    """Remove Lombok import statements."""
    # Remove import lombok.* statements
    content = re.sub(r'import\s+lombok\.\w+;\s*\n', '', content)
    return content

def remove_lombok_annotations(content):
    """Remove Lombok annotations from content."""
    lombok_annotations = ['Data', 'Builder', 'NoArgsConstructor', 'AllArgsConstructor',
                          'Slf4j', 'Getter', 'Setter', 'EqualsAndHashCode', 'RequiredArgsConstructor']

    for annotation in lombok_annotations:
        # Remove annotation with optional parameters
        content = re.sub(r'@' + annotation + r'(?:\([^)]*\))?\s*\n', '', content)

    return content

def generate_getter(field_type, field_name):
    """Generate getter method for a field."""
    capitalized_name = field_name[0].upper() + field_name[1:]
    if field_type == 'boolean':
        return f"""    public {field_type} is{capitalized_name}() {{
        return {field_name};
    }}
"""
    else:
        return f"""    public {field_type} get{capitalized_name}() {{
        return {field_name};
    }}
"""

def generate_setter(field_type, field_name):
    """Generate setter method for a field."""
    capitalized_name = field_name[0].upper() + field_name[1:]
    return f"""    public void set{capitalized_name}({field_type} {field_name}) {{
        this.{field_name} = {field_name};
    }}
"""

def generate_no_args_constructor(class_name, fields):
    """Generate no-args constructor."""
    return f"""    public {class_name}() {{
    }}
"""

def generate_all_args_constructor(class_name, fields):
    """Generate all-args constructor."""
    if not fields:
        return ""

    params = ", ".join([f"{f['type']} {f['name']}" for f in fields])
    assignments = "\n        ".join([f"this.{f['name']} = {f['name']};" for f in fields])

    return f"""    public {class_name}({params}) {{
        {assignments}
    }}
"""

def generate_builder_class(class_name, fields):
    """Generate builder class."""
    if not fields:
        return ""

    builder_fields = "\n        ".join([f"private {f['type']} {f['name']};" for f in fields])

    builder_methods = ""
    for f in fields:
        capitalized_name = f['name'][0].upper() + f['name'][1:]
        builder_methods += f"""        public {class_name}Builder {f['name']}({f['type']} {f['name']}) {{
            this.{f['name']} = {f['name']};
            return this;
        }}
"""

    build_params = ", ".join([f"{f['name']}" for f in fields])

    return f"""    public static {class_name}Builder builder() {{
        return new {class_name}Builder();
    }}

    public static class {class_name}Builder {{
        {builder_fields}

{builder_methods}
        public {class_name} build() {{
            return new {class_name}({build_params});
        }}
    }}
"""

def generate_logger(class_name, package):
    """Generate SLF4J logger declaration."""
    return f"""    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger({class_name}.class);
"""

def convert_file(filepath):
    """Convert a single Java file from Lombok to explicit code."""
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    class_name = extract_class_name(content)
    if not class_name:
        print(f"Could not find class name in {filepath}")
        return False

    package = extract_package(content)

    # Check which Lombok annotations are present
    has_data = has_lombok_annotation(content, 'Data')
    has_builder = has_lombok_annotation(content, 'Builder')
    has_no_args = has_lombok_annotation(content, 'NoArgsConstructor')
    has_all_args = has_lombok_annotation(content, 'AllArgsConstructor')
    has_getter = has_lombok_annotation(content, 'Getter')
    has_setter = has_lombok_annotation(content, 'Setter')
    has_sl4j = has_lombok_annotation(content, 'Slf4j')

    if not any([has_data, has_builder, has_no_args, has_all_args, has_getter, has_setter, has_sl4j]):
        print(f"No Lombok annotations found in {filepath}")
        return False

    print(f"Converting {filepath}...")
    print(f"  - Class: {class_name}")
    print(f"  - @Data: {has_data}, @Builder: {has_builder}, @Slf4j: {has_sl4j}")

    # Extract fields
    fields = extract_fields(content)

    # Remove Lombok imports and annotations
    new_content = remove_lombok_imports(content)
    new_content = remove_lombok_annotations(new_content)

    # Find the position after the class declaration
    class_decl_pattern = r'(public\s+(?:class|enum|interface)\s+' + class_name + r'[^\{]*\{)'
    class_decl_match = re.search(class_decl_pattern, new_content)

    if not class_decl_match:
        print(f"Could not find class declaration in {filepath}")
        return False

    insert_pos = class_decl_match.end()

    # Generate code to insert
    insert_code = "\n"

    # Add logger if @Slf4j was present
    if has_sl4j:
        insert_code += generate_logger(class_name, package)
        insert_code += "\n"

    # Add no-args constructor
    if has_no_args or has_data:
        insert_code += generate_no_args_constructor(class_name, fields)
        insert_code += "\n"

    # Add all-args constructor
    if has_all_args or has_data:
        insert_code += generate_all_args_constructor(class_name, fields)
        insert_code += "\n"

    # Add builder
    if has_builder or has_data:
        insert_code += generate_builder_class(class_name, fields)
        insert_code += "\n"

    # Add getters and setters for @Data
    if has_data:
        for field in fields:
            insert_code += generate_getter(field['type'], field['name'])
            insert_code += "\n"
            insert_code += generate_setter(field['type'], field['name'])
            insert_code += "\n"

    # Insert the generated code
    new_content = new_content[:insert_pos] + insert_code + new_content[insert_pos:]

    # Write the converted file
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(new_content)

    print(f"  - Converted successfully!")
    return True

def main():
    if len(sys.argv) < 2:
        print("Usage: python convert_lombok.py <directory>")
        sys.exit(1)

    target_dir = sys.argv[1]

    # Find all Java files with Lombok annotations
    java_files = []
    for root, dirs, files in os.walk(target_dir):
        for file in files:
            if file.endswith('.java'):
                filepath = os.path.join(root, file)
                with open(filepath, 'r', encoding='utf-8') as f:
                    content = f.read()
                if any(annotation in content for annotation in ['@Data', '@Builder', '@Slf4j', '@NoArgsConstructor', '@AllArgsConstructor', '@Getter', '@Setter']):
                    java_files.append(filepath)

    print(f"Found {len(java_files)} files with Lombok annotations")

    converted = 0
    for filepath in java_files:
        try:
            if convert_file(filepath):
                converted += 1
        except Exception as e:
            print(f"Error converting {filepath}: {e}")

    print(f"\nConverted {converted} files successfully")

if __name__ == '__main__':
    main()
