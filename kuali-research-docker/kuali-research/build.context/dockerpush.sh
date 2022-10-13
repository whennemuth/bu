# login to ECR (requires ~/.aws/config)
eval $(aws ecr get-login --profile sandbox)

# Tag the image
# Example of repository: 730096353738.dkr.ecr.us-east-1.amazonaws.com/coeus
docker tag bu-ist/kuali-research "$(cat repository)"

# Push the image
docker push "$(cat repository)"
