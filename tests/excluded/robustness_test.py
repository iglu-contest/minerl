# Simple env test.
import json
import select
import time
import logging

import gym
import matplotlib.pyplot as plt
import minerl_patched
import numpy as np
from minerl_patched.env.core import MineRLEnv

import coloredlogs

coloredlogs.install(logging.DEBUG)

# This test should constantly attempt to reconnect and occasionally restart the service
NUM_EPISODES = 10


def main():
    """
    Tests running a simple environment.
    """
    old_wait, old_timeout = minerl_patched.env.core.MAX_WAIT, minerl_patched.env.core.SOCKTIME
    minerl_patched.env.core.MAX_WAIT, minerl_patched.env.core.SOCKTIME = 1, 5.0

    env = gym.make('MineRLNavigateDense-v0')

    for _ in range(NUM_EPISODES):
        obs = env.reset()
        done = False
        while not done:
            random_act = env.action_space.sample()

            obs, reward, done, info = env.step(
                random_act)

    minerl_patched.env.core.MAX_WAIT, minerl_patched.env.core.SOCKTIME = old_wait, old_timeout


if __name__ == "__main__":
    main()
