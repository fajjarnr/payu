import type { FormInstance } from 'antd';
import type { Rule } from 'antd/es/form';
import type { ZodType } from 'zod';

/**
 * Satu pola validasi form untuk semua form antd: `rules={[{ validator }]}`
 * yang didukung skema zod bestehende. Validator mem-parse seluruh nilai form
 * (sehingga refinement lintas-field seperti `confirmPassword` tetap jalan)
 * lalu hanya melempar error untuk field-nya sendiri.
 */
export function zodFieldRule(form: FormInstance, schema: ZodType, field: string): Rule {
  return {
    validator: async () => {
      const parsed = schema.safeParse(form.getFieldsValue());
      if (parsed.success) return;
      const issue = parsed.error.issues.find((i) => String(i.path[0]) === field);
      if (issue) throw new Error(issue.message);
    },
  };
}
