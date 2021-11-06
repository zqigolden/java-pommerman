import json
import sys
import os

json_data:dict = json.load(open(sys.argv[1]))

java_cmds = [f'java -jar out/eval/java-pommerman.jar {0} {10} {4} {-1} {0} {7} {0} {5}']
java_cmds.append(json_data.get("num_iterations", 200))
java_cmds.append(json_data.get("rollout_depth", 12))
java_cmds.append(json_data.get("uct_method", "UCT_UCB1"))
java_cmds.append(json_data.get("safe_place_method", "SAFE_PLACE_DEFAULT"))
java_cmds.append(json_data.get("heuristic_method", "CUSTOM_HEURISTIC"))
java_cmds.append(json_data.get("uctBias", 0.0))
java_cmds.append(json_data.get("progressiveUnpruningEnable", 'false'))
java_cmds.append(json_data.get("progressiveUnpruningT", 30))
java_cmds.append(json_data.get("progressiveUnpruningKInit", 4))
java_cmds.append(json_data.get("progressiveUnpruningA", 50))
java_cmds.append(json_data.get("progressiveUnpruningB", 1.3))

java_cmds = ' '.join(map(str, java_cmds))
print(java_cmds)
os.system(java_cmds)
