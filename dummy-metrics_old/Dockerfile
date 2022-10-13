FROM node:8

RUN \
  apt-get update -y && \
  apt-get install -y apt-file && \
  apt-file update -y && \
  apt-get install -y vim

WORKDIR /app
COPY package*.json ./
RUN npm install