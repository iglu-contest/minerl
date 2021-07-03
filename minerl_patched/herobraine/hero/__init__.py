# Copyright (c) 2020 All Rights Reserved
# Author: William H. Guss, Brandon Houghton

"""
minerl_patched.herobraine.hero -- The interface between Hero (Malmo) and the minerl_patched.herobraine package.
"""

import logging

logger = logging.getLogger(__name__)

import minerl_patched.herobraine.hero.mc
import minerl_patched.herobraine.hero.spaces

from minerl_patched.herobraine.hero.mc import KEYMAP
