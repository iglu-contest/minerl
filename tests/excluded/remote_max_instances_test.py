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
from minerl_patched.env.malmo import InstanceManager

import coloredlogs

coloredlogs.install(logging.INFO)


def main():
    """
    Tests running a simple environment.
    """
    #    InstanceManager.MAXINSTANCES = 1
    env = gym.make('MineRLNavigateDense-v0')
    try:
        gym.make('MineRLObtainDiamondDense-v0')
        assert False, "Did not throw an exception."
    except RuntimeError:
        pass

    print("Demo complete.")


if __name__ == "__main__":
    main()
