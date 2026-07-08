# Cluster Information
output "cluster_name" {
  description = "EKS cluster name"
  value       = aws_eks_cluster.main.name
}

output "cluster_endpoint" {
  description = "EKS cluster endpoint"
  value       = aws_eks_cluster.main.endpoint
}

output "cluster_security_group_id" {
  description = "Security group ID attached to the EKS cluster"
  value       = aws_security_group.eks_cluster.id
}

output "cluster_arn" {
  description = "EKS cluster ARN"
  value       = aws_eks_cluster.main.arn
}

output "cluster_certificate_authority_data" {
  description = "Base64 encoded certificate data required to communicate with the cluster"
  value       = aws_eks_cluster.main.certificate_authority[0].data
  sensitive   = true
}

output "cluster_version" {
  description = "Kubernetes version of the cluster"
  value       = aws_eks_cluster.main.version
}

# Region
output "region" {
  description = "AWS region where resources are deployed"
  value       = var.aws_region
}

# VPC Information
output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.main.id
}

output "vpc_cidr" {
  description = "VPC CIDR block"
  value       = aws_vpc.main.cidr_block
}

# Subnet Information
output "public_subnet_ids" {
  description = "List of public subnet IDs"
  value       = aws_subnet.public[*].id
}

output "public_subnet_cidrs" {
  description = "List of public subnet CIDR blocks"
  value       = aws_subnet.public[*].cidr_block
}

# Node Group Information
output "node_group_name" {
  description = "EKS node group name"
  value       = aws_eks_node_group.main.node_group_name
}

output "node_group_id" {
  description = "EKS node group ID"
  value       = aws_eks_node_group.main.id
}

output "node_group_status" {
  description = "Status of the EKS node group"
  value       = aws_eks_node_group.main.status
}

output "node_security_group_id" {
  description = "Security group ID attached to the EKS nodes"
  value       = aws_security_group.eks_nodes.id
}

output "node_role_arn" {
  description = "IAM role ARN for EKS nodes"
  value       = aws_iam_role.eks_nodes.arn
}

# OIDC Provider Information
output "oidc_provider_arn" {
  description = "ARN of the OIDC Provider for EKS"
  value       = aws_iam_openid_connect_provider.eks.arn
}

output "oidc_provider_url" {
  description = "URL of the OIDC Provider"
  value       = aws_iam_openid_connect_provider.eks.url
}

output "aws_load_balancer_controller_role_arn" {
  description = "IAM role ARN for AWS Load Balancer Controller"
  value       = aws_iam_role.aws_load_balancer_controller.arn
}

output "ebs_csi_driver_role_arn" {
  description = "IAM role ARN for EBS CSI Driver"
  value       = aws_iam_role.ebs_csi_driver.arn
}

# kubectl Configuration Command
output "configure_kubectl" {
  description = "Command to update kubeconfig for kubectl access"
  value       = "aws eks update-kubeconfig --region ${var.aws_region} --name ${aws_eks_cluster.main.name}"
}

# Deployment Summary
output "deployment_summary" {
  description = "Summary of the EKS deployment"
  value       = <<-EOT
    ========================================
    EKS Cluster Deployment Summary
    ========================================
    Cluster Name:     ${aws_eks_cluster.main.name}
    Region:           ${var.aws_region}
    Kubernetes:       ${aws_eks_cluster.main.version}
    VPC ID:           ${aws_vpc.main.id}
    Subnets:          ${join(", ", aws_subnet.public[*].id)}
    Node Group:       ${aws_eks_node_group.main.node_group_name}
    Instance Type:    ${var.node_instance_type}
    Nodes (min/des/max): ${var.node_min_size}/${var.node_desired_size}/${var.node_max_size}
    
    OIDC Provider:    ${aws_iam_openid_connect_provider.eks.arn}
    LB Controller Role: ${aws_iam_role.aws_load_balancer_controller.arn}
    
    Configure kubectl:
    aws eks update-kubeconfig --region ${var.aws_region} --name ${aws_eks_cluster.main.name}
    
    Verify cluster:
    kubectl get nodes
    kubectl get pods -A
    ========================================
  EOT
}
