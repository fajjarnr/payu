#!/bin/bash
roles=(
  "payu-onprem-aws-ebs-csi-driver-controller"
  "payu-onprem-cloud-controller"
  "payu-onprem-cloud-network-config-controller"
  "payu-onprem-control-plane-operator"
  "payu-onprem-hcp-cli-role"
  "payu-onprem-node-pool"
  "payu-onprem-odf-noobaa"
  "payu-onprem-openshift-image-registry"
  "payu-onprem-openshift-ingress"
  "payu-prod-aws-ebs-csi-driver-controller"
  "payu-prod-cloud-controller"
  "payu-prod-cloud-network-config-controller"
  "payu-prod-control-plane-operator"
  "payu-prod-hcp-cli-role"
  "payu-prod-node-pool"
  "payu-prod-openshift-image-registry"
  "payu-prod-openshift-ingress"
)

for role in "${roles[@]}"; do
  echo "Deleting role: $role"
  # Detach managed policies
  attached_policies=$(aws iam list-attached-role-policies --role-name "$role" --query "AttachedPolicies[].PolicyArn" --output text 2>/dev/null)
  for policy in $attached_policies; do
    echo "  Detaching policy: $policy"
    aws iam detach-role-policy --role-name "$role" --policy-arn "$policy" 2>/dev/null
  done
  
  # Delete inline policies
  inline_policies=$(aws iam list-role-policies --role-name "$role" --query "PolicyNames[]" --output text 2>/dev/null)
  for policy in $inline_policies; do
    echo "  Deleting inline policy: $policy"
    aws iam delete-role-policy --role-name "$role" --policy-name "$policy" 2>/dev/null
  done
  
  # Delete instance profiles if any associated
  profiles=$(aws iam list-instance-profiles-for-role --role-name "$role" --query "InstanceProfiles[].InstanceProfileName" --output text 2>/dev/null)
  for profile in $profiles; do
    echo "  Removing from instance profile: $profile"
    aws iam remove-role-from-instance-profile --instance-profile-name "$profile" --role-name "$role" 2>/dev/null
    # Optionally delete instance profile if it matches the role name pattern
    if [[ "$profile" == *"payu-onprem"* || "$profile" == *"payu-prod"* ]]; then
      echo "  Deleting instance profile: $profile"
      aws iam delete-instance-profile --instance-profile-name "$profile" 2>/dev/null
    fi
  done

  # Delete role
  aws iam delete-role --role-name "$role" 2>/dev/null
done
echo "IAM Cleanup Done!"
