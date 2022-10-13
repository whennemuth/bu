# login to ECR (requires ~/.aws/config)
eval $(aws ecr get-login --profile sandbox)

# Tag the image
docker tag hello-world:v001 730096353738.dkr.ecr.us-east-1.amazonaws.com/hello-world:v001

# Push the image
docker push 730096353738.dkr.ecr.us-east-1.amazonaws.com/hello-world:v001