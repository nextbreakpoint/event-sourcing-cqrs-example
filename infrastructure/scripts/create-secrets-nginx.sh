kubectl create secret generic nginx-config --from-file=../nginx/nginx.conf --from-file=../nginx/nginx.crt --from-file=../nginx/nginx.key
