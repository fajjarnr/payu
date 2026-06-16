resource "aws_key_pair" "bastion_key" {
  key_name   = "bastion-key"
  public_key = file(pathexpand(var.bastion_public_key))
}

module "bastion" {
  source = "./modules/ec2"

  instance_name               = local.names.bastion
  instance_type               = "m6a.4xlarge"
  os_name                     = "ubuntu"
  root_volume_size            = 100
  root_volume_type            = "gp3"
  key_name                    = aws_key_pair.bastion_key.key_name
  subnet_id                   = module.vpc.public_subnet_ids[0]
  vpc_security_group_ids      = [module.vpc.security_group_id]
  associate_public_ip_address = true
  enable_ssm                  = true
  user_data                   = <<-EOF
              #!/bin/bash
              apt-get update -y
              apt-get install -y unzip
              curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
              unzip awscliv2.zip
              sudo ./aws/install
              EOF
  tags                        = local.common_tags
  # Catatan: Bila hanya ingin akses via SSM Session Manager,
  # Anda bisa menonaktifkan SSH key dengan menghapus/komentari "key_name".

  depends_on = [module.vpc]
}
