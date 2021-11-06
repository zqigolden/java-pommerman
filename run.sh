CPU=4
ls configs | xargs -I{} -P ${CPU} sh -c 'python3 run_eval.py configs/{} > expout/{}.out'