CPU=4
ls configs | grep pruning | xargs -I{} -P ${CPU} sh -c 'python3 run_eval.py configs/{} > expout/{}.out'