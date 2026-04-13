# Infrastruktur AWS dengan Terraform

Repo ini men-deploy komponen VPC, optional Bastion (EC2), dan optional EKS menggunakan Terraform modular dengan best practices untuk production.

## Fitur Utama

- **Security-First**: EBS encryption, EKS secrets encryption, IMDSv2, cluster logging
- **High Availability**: Multi-AZ deployment, NAT Gateway, lifecycle management
- **Cost-Optimized**: GP3 volumes, taggable resources untuk cost tracking
- **Production-Ready**: Lifecycle policies, update configs, version pinning
- **Flexible Tagging**: Tag inheritance dan merge di semua resources

## Prasyarat

- Terraform >= 1.5.0
- AWS CLI terkonfigurasi dengan kredensial yang valid
- Permissions untuk membuat VPC, EC2, EKS, IAM roles

## Quick Start

### 1. Clone dan Setup Variables

```bash
git clone <repository-url>
cd infra-auto
cp terraform.tfvars.example terraform.tfvars
```

### 2. Edit terraform.tfvars

```hcl
bastion_public_key = "ssh-ed25519 AAAA..."

name_prefix = "openshift"
environment = "production"
region      = "ap-southeast-1"

public_subnets = [
  { cidr = "10.0.0.0/20" },
  { cidr = "10.0.16.0/20" },
  { cidr = "10.0.32.0/20" }
]

private_subnets = [
  { cidr = "10.0.48.0/20" },
  { cidr = "10.0.64.0/20" },
  { cidr = "10.0.80.0/20" }
]

default_tags = {
  Project     = "infra-auto"
  Owner       = "your-name"
  Environment = "production"
  ManagedBy   = "terraform"
}
```

> **Tip:** `name_prefix` dan `environment` otomatis digabung menjadi prefix penamaan (contoh: `openshift-production-vpc`). Ubah dua variabel ini untuk mengganti nama resource di seluruh modul secara konsisten.

### 3. Deploy Infrastructure

```bash
# Initialize Terraform
terraform init

# Format code
terraform fmt -recursive

# Validate configuration
terraform validate

# Preview changes
terraform plan

# Apply infrastructure
terraform apply
```

## Kunci SSH untuk Bastion

Jika mengaktifkan Bastion module (contoh di `02-bastion.tf`), sediakan kunci publik SSH.

**Membuat SSH Key Pair:**

```bash
ssh-keygen -t ed25519 -C "your_email@example.com" -f ~/.ssh/bastion-key
```

**Set Public Key di terraform.tfvars:**

```hcl
bastion_public_key = "ssh-ed25519 AAAA... your_email@example.com"
```

**Connect ke Bastion:**

```bash
# Via SSH
ssh -i ~/.ssh/bastion-key ubuntu@<bastion-public-ip>

# Via SSM Session Manager (tanpa SSH key)
aws ssm start-session --target <instance-id>
```

**PENTING**:
- Jangan commit `terraform.tfvars` atau private key ke version control
- File ini sudah ada di `.gitignore`

## Module Documentation

### VPC Module (`modules/vpc`)

Membuat VPC dengan public/private subnets, Internet Gateway, NAT Gateway (optional), dan S3 VPC Endpoint.

**Features:**
- Multi-AZ deployment (3 AZs default)
- Public dan private subnets dengan route tables terpisah
- NAT Gateway untuk internet access dari private subnets
- S3 VPC Endpoint untuk akses S3 tanpa NAT charges
- Kubernetes-ready tags untuk ELB integration
- Tagging support penuh

**Key Variables:**
```hcl
vpc_cidr             = "10.0.0.0/16"           # VPC CIDR block
public_subnets = [                            # Public subnets (optional AZ override)
  { cidr = "10.0.0.0/20", availability_zone = "ap-southeast-1a" },
  { cidr = "10.0.16.0/20", availability_zone = "ap-southeast-1b" }
]
private_subnets = [                           # Private subnets
  { cidr = "10.0.48.0/20", availability_zone = "ap-southeast-1a" },
  { cidr = "10.0.64.0/20", availability_zone = "ap-southeast-1b" }
]
enable_nat_gateway   = true                    # Enable NAT Gateway
region              = "ap-southeast-1"         # AWS region
```

**Outputs:**
- `vpc_id`: VPC ID
- `public_subnet_ids`: Public subnet IDs
- `private_subnet_ids`: Private subnet IDs
- `security_group_id`: Default security group ID
- `nat_gateway_id`: NAT Gateway ID (if enabled)
- `internet_gateway_id`: Internet Gateway ID

### EC2 Module (`modules/ec2`)

Instance EC2 dengan SSM support, multiple OS options, dan EBS encryption.

**Features:**
- **OS Support**: Ubuntu 24.04, Amazon Linux 2023, RHEL 9
- **Architecture**: AMD64 dan ARM64 (Graviton)
- **Security**: EBS encryption, IMDSv2 required, SSM access
- **Storage**: GP3 volumes dengan configurable size
- **IAM**: Auto-provisioned SSM role dan instance profile

**Key Variables:**
```hcl
instance_name               = "bastion"        # Instance name
instance_type               = "t3.medium"      # Instance type
os_name                     = "ubuntu"         # OS: ubuntu, amazon_linux, rhel9
instance_architecture       = "amd64"          # amd64 or arm64
root_volume_size            = 30               # Root volume size (GB)
root_volume_type            = "gp3"            # Volume type (gp3 recommended)
enable_ssm                  = true             # Enable SSM access
associate_public_ip_address = true             # Assign public IP
```

**Outputs:**
- `instance_id`: EC2 instance ID
- `instance_public_ip`: Public IP address
- `instance_private_ip`: Private IP address

### EKS Module (`modules/eks`)

Production-ready EKS cluster dengan security, logging, dan lifecycle management.

**Features:**
- **Security**: KMS secrets encryption (optional), endpoint access controls, cluster logging
- **Logging**: CloudWatch logs untuk API, audit, authenticator, controller, scheduler
- **Addons**: VPC CNI, CoreDNS, kube-proxy dengan version pinning
- **Node Groups**: Auto-scaling dengan lifecycle management
- **Updates**: Zero-downtime updates dengan `create_before_destroy`

**Key Variables:**
```hcl
cluster_name            = "my-eks-cluster"     # Cluster name
cluster_version         = "1.30"               # Kubernetes version
instance_type           = "t3.medium"          # Node instance type
desired_capacity        = 3                    # Desired node count
min_size                = 2                    # Minimum nodes
max_size                = 5                    # Maximum nodes

# Security
endpoint_private_access = true                 # Private API access
endpoint_public_access  = true                 # Public API access
public_access_cidrs     = ["0.0.0.0/0"]       # API access CIDRs
kms_key_arn            = ""                    # KMS key for encryption (optional)

# Logging
enabled_cluster_log_types = [                  # Enable all logs
  "api", "audit", "authenticator",
  "controllerManager", "scheduler"
]

# Addons (optional version pinning)
addons = {
  "vpc-cni" = { version = null },
  "coredns" = { version = null },
  "kube-proxy" = { version = null }
}

# Node Configuration
node_disk_size = 20                            # Node disk size (GB)
node_labels    = {}                            # Kubernetes node labels
```

**Outputs:**
- `cluster_name`: EKS cluster name
- `cluster_endpoint`: Kubernetes API endpoint
- `cluster_ca_certificate`: Cluster CA certificate
- `node_group_name`: Node group name

## Security Features

### Implemented Best Practices

1. **EBS Encryption**
   - All EC2 root volumes encrypted at rest
   - Automatic encryption dengan AWS managed keys
   - Delete on termination enabled

2. **EKS Security**
   - Optional KMS encryption untuk Kubernetes secrets
   - Cluster logging enabled (API, audit, authenticator, controller, scheduler)
   - VPC endpoint access controls
   - Public access CIDR restrictions

3. **IMDSv2 Required**
   - EC2 metadata service v2 required untuk semua instances
   - Protection against SSRF attacks

4. **IAM Best Practices**
   - Least privilege IAM roles
   - Auto-provisioned SSM roles untuk EC2
   - Proper role segregation untuk EKS cluster dan nodes

5. **Network Security**
   - Private subnets dengan NAT Gateway
   - S3 VPC Endpoint untuk secure S3 access
   - Security groups customizable per environment

### Security Notes

**Lab/Development**: Security Group default di VPC module mengizinkan semua inbound traffic (`0.0.0.0/0`) untuk keperluan lab.

**Production**: Update security group rules di VPC module:

```hcl
# modules/vpc/main.tf
resource "aws_security_group" "main" {
  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["10.0.0.0/16"]  # Restrict to VPC only
  }

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["YOUR_IP/32"]    # Restrict SSH to specific IPs
  }
}
```

## Advanced Configuration

### Custom Tagging

Semua modules mendukung custom tagging:

```hcl
module "bastion" {
  source = "./modules/ec2"
  # ... other variables ...

  tags = {
    Application = "bastion-host"
    CostCenter  = "engineering"
    Compliance  = "required"
  }
}
```

Tags akan di-merge dengan `default_tags` dari provider dan Name tag.

### EKS with KMS Encryption

```hcl
# Create KMS key first
resource "aws_kms_key" "eks" {
  description             = "EKS cluster encryption key"
  deletion_window_in_days = 7
}

module "eks" {
  source = "./modules/eks"
  # ... other variables ...

  kms_key_arn = aws_kms_key.eks.arn
}
```

### Custom EKS Addon Versions

```hcl
module "eks" {
  source = "./modules/eks"
  # ... other variables ...

  addons = {
    "vpc-cni"    = { version = "v1.15.1-eksbuild.1" }
    "coredns"    = { version = "v1.10.1-eksbuild.2" }
    "kube-proxy" = { version = "v1.28.1-eksbuild.1" }
  }
}
```

### Multi-Architecture EC2

```hcl
# ARM64 instance (Graviton)
module "bastion_arm" {
  source = "./modules/ec2"

  instance_name         = "bastion-arm"
  instance_type         = "t4g.medium"        # Graviton instance
  instance_architecture = "arm64"
  os_name              = "ubuntu"
}
```

## Remote State (Optional)

Untuk production, uncomment backend configuration di `backend.tf`:

```hcl
terraform {
  backend "s3" {
    bucket         = "your-terraform-state-bucket"
    key            = "infra-auto/terraform.tfstate"
    region         = "ap-southeast-1"
    dynamodb_table = "terraform-lock-table"
    encrypt        = true
  }
}
```

**Create S3 bucket and DynamoDB table:**

```bash
# Create S3 bucket
aws s3api create-bucket \
  --bucket your-terraform-state-bucket \
  --region ap-southeast-1 \
  --create-bucket-configuration LocationConstraint=ap-southeast-1

# Enable versioning
aws s3api put-bucket-versioning \
  --bucket your-terraform-state-bucket \
  --versioning-configuration Status=Enabled

# Create DynamoDB table for locking
aws dynamodb create-table \
  --table-name terraform-lock-table \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST
```

## Lifecycle Management

### EKS Node Group Updates

Node groups menggunakan `create_before_destroy` lifecycle untuk zero-downtime updates:

```hcl
# Automatic behavior - no changes needed
lifecycle {
  create_before_destroy = true
  ignore_changes        = [scaling_config[0].desired_size]
}
```

Ini memungkinkan:
- Rolling updates tanpa downtime
- Auto-scaling tidak di-override oleh Terraform
- Safe instance type changes

## Cost Optimization

1. **GP3 Volumes**: 20% lebih murah dari GP2 dengan performa sama/lebih baik
2. **NAT Gateway**: Optional - disable untuk dev environments
3. **Tagging**: Comprehensive tagging untuk cost allocation
4. **Graviton Instances**: ARM64 instances 20% lebih murah

**Disable NAT for Dev:**

```hcl
module "vpc" {
  source = "./modules/vpc"
  # ... other variables ...

  enable_nat_gateway = false  # Save ~$32/month per NAT Gateway
}
```

## Troubleshooting

### EKS Cluster Access

```bash
# Update kubeconfig
aws eks update-kubeconfig --name my-eks-cluster --region ap-southeast-1

# Verify access
kubectl get nodes
```

### SSM Access Issues

```bash
# Check instance SSM status
aws ssm describe-instance-information

# Verify IAM role
aws iam get-role --role-name bastion-ssm-role
```

### Terraform State Issues

```bash
# Refresh state
terraform refresh

# Import existing resource
terraform import aws_instance.bastion i-1234567890abcdef0
```

## Project Structure

```
.
├── 01-vpc.tf              # VPC module call
├── 02-bastion.tf          # Bastion EC2 module call
├── 03-eks.tf              # EKS module call
├── backend.tf             # S3 backend config (commented)
├── providers.tf           # AWS provider config
├── variables.tf           # Root variables
├── outputs.tf             # Root outputs
├── versions.tf            # Terraform & provider versions
├── terraform.tfvars       # User variables (gitignored)
├── terraform.tfvars.example  # Example variables
└── modules/
    ├── vpc/              # VPC module
    ├── ec2/              # EC2 module
    └── eks/              # EKS module
```

## Contributing

1. Run `terraform fmt -recursive` before committing
2. Validate with `terraform validate`
3. Update documentation for new features
4. Test in non-production environment first

## License

[Add your license here]
