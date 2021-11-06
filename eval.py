import argparse
import json

# parser = argparse.ArgumentParser()
# parser.add_argument('--num_iterations', default=200, type=int, required=False)
# parser.add_argument('--rollout_depth', default=12, type=int, required=False)
# parser.add_argument('--uct_method', default='UCT_UCB1', required=False)
# parser.add_argument('--safe_place_method', default='SAFE_PLACE_DEFAULT', required=False)
# parser.add_argument('--heuristic_method', default='CUSTOM_HEURISTIC', required=False)
# parser.add_argument('--uctBias', default=0, type=float, required=False)
# parser.add_argument('--progressiveUnpruningEnable', default='false', required=False)
# parser.add_argument('--progressiveUnpruningT', default=30, type=int, required=False)
# parser.add_argument('--progressiveUnpruningKInit', default=4, type=int, required=False)
# parser.add_argument('--progressiveUnpruningA', default=50., type=float, required=False)
# parser.add_argument('--progressiveUnpruningB', default=1.3, type=float, required=False)
# args = parser.parse_args()
# json.dump({i:j for i, j in args._get_kwargs()}, open('configs/tmp.json', 'w'), indent=4)

options = dict(
    num_iterations = [100, 200, 350, 300, 450],
    rollout_depth = [10, 12, 14],
    uct_method = ['UCT_UCB1', 'UCT_UCB1_TURNED'],
    safe_place_method = ['SAFE_PLACE_DEFAULT', 'SAFE_PLACE_EARLY_STOP', 'SAFE_PLACE_ADVANCED'],
    heuristic_method = ['CUSTOM_HEURISTIC', 'ADVANCED_HEURISTIC'],
    uctBias = [0, 0.1, 0.5, 1.0, 1.5, 2.0]
)

for k, vs in options.items():
    with open("configs/default.json") as f:
        data = json.load(f)
    for v in vs:
        data[k] = v
        json.dump(data, open(f'configs/ctl_{k}_{v}.json', 'w'), indent=4)
