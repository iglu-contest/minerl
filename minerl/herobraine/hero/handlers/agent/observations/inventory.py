# Copyright (c) 2020 All Rights Reserved
# Author: William H. Guss, Brandon Houghton

import abc
import logging
from typing import List, Optional, Sequence

from minerl.herobraine.hero.handlers.translation import TranslationHandler
import numpy as np
from minerl.herobraine.hero import mc, spaces


def _univ_obs_get_inventory_slots(obs: dict) -> Optional[List[dict]]:
    """
    Observations in univ.json contain "slot dicts" containing info about
    items held in player or container inventories. This function processes an obs dict
    from univ.json, and returns
    the list of "slot" dictionaries, where every non-empty dictionary corresponds to
    an item stack.

    See the following link for an example format:
    https://gist.github.com/shwang/9c8815e952eb2a4c308aea09f112cd6a#file-univ-head-json-L162
    """
    # If these keys don't exist, then we should KeyError.
    gui_type = obs['slots']['gui']['type']
    gui_slots = obs['slots']['gui']['slots']

    if gui_type in ('class net.minecraft.inventory.ContainerPlayer',
                    'class net.minecraft.inventory.ContainerWorkbench'):
        slots = gui_slots[1:]
    elif gui_type == 'class net.minecraft.inventory.ContainerFurnace':
        slots = gui_slots[0:2] + gui_slots[3:]
    else:
        slots = gui_slots

    # Add in the cursor item tracking only if present.
    cursor_item = gui_slots.get('cursor_item')
    if cursor_item is not None:
        slots.append(cursor_item)
    return slots


class InventoryObservationABC(TranslationHandler, abc.ABC):

    def __init__(self, item_list: Sequence[str]):
        """
        Args:
            item_list: List of string item identifiers, used as keys for the
                associated inventory Dict space. Each key in the Dict space is
                mapped to a log-scale Box space.
        """
        # TODO: Add item validation? Could cross-reference against
        # mc_constants.json or Malmo/Schemas/Types.xsd.
        item_list = sorted(item_list)
        self.items = item_list
        self.num_items = len(item_list)
        super().__init__(spaces.Dict(spaces={
            k: spaces.Box(low=0, high=2304,
                          shape=(), dtype=np.int32, normalizer_scale='log')
            for k in item_list
        }))

    def to_string(self):
        return 'inventory'

    def xml_template(self) -> str:
        return str("""<ObservationFromFullInventory flat="false"/>""")

    @abc.abstractmethod
    def from_hero(self, x: dict):
        pass

    @abc.abstractmethod
    def to_hero(self, x: dict):
        pass

    def __or__(self, other):
        """
        Combines two flat inventory observations into one by taking the
        union of their items.
        Asserts that other is also a flat observation.
        """
        cls = type(self)
        assert isinstance(other, cls)
        return cls(list(set(self.items) | (set(other.items))))

    def __eq__(self, other):
        return isinstance(other, type(self)) and (self.items) == (other.items)


class FlatInventoryObservation(InventoryObservationABC):
    """Creates an 'inventory' Dict observation from item name to item count,
    by handling GUI Container Observations for the selected items.

    Does not recognize item variants (all items of the same type and different
    variants are grouped into the same category). See
    FlatInventoryVariantObservation if your environment requires variant-aware
    observations.
    """

    logger = logging.getLogger(__name__ + ".FlatInventoryObservation")

    def __init__(self, item_list: Sequence[str]):
        """
        Args:
            item_list: String item identifiers without variant
                suffixes. No item identifier can contain '#' (special delimiter
                for variant number).
        """
        assert len(item_list) == len(set(item_list))
        for x in item_list:
            assert '#' not in x
        super().__init__(item_list)

    def add_to_mission_spec(self, mission_spec):
        pass
        # Flat obs not supported by API for some reason - should be mission_spec.observeFullInventory(flat=True)

    def from_hero(self, info):
        """
        Converts the Hero observation into a one-hot of the inventory items
        for a given inventory container. Ignores variant / color
        :param obs:
        :return:
        """
        item_dict = self.space.no_op()
        # TODO: RE-ADDRESS THIS DUCK TYPED INVENTORY DATA FORMAT WHEN MOVING TO STRONG TYPING
        for stack in info['inventory']:
            if 'type' in stack and 'quantity' in stack:
                parts, _ = stack['type'].split('#')  # Expecting exactly one '#'
                if type_name == 'log2' and 'log2' not in self.items:
                    type_name = 'log'
                if type_name in item_dict:
                    # This sets the number of air to correspond to the number of empty slots :)
                    if type_name == "air":
                        item_dict[type_name] += 1
                    else:
                        item_dict[type_name] += stack["quantity"]

        return item_dict

    def from_universal(self, obs):
        item_dict = self.space.no_op()

        try:
            slots = _univ_obs_get_inventory_slots(obs)

            # Add from all slots
            for stack in slots:
                try:
                    name = mc.strip_item_prefix(stack['name'])
                    name = 'log' if name == 'log2' else name
                    if name == "air":
                        item_dict[name] += 1
                    else:
                        item_dict[name] += stack['count']
                except (KeyError, ValueError):
                    continue

        except KeyError as e:
            self.logger.warning("KeyError found in universal observation! Yielding empty inventory.")
            self.logger.error(e)
            return item_dict

        return item_dict


def _get_variant_item_name(item_type: str, variant: str) -> str:
    return f"{item_type}#{variant}"


class FlatInventoryVariantObservation(FlatInventoryObservation):
    """Creates an 'inventory' Dict observation from item name + variant number
    to item count, by handling GUI Container Observations for the selected items.
    """

    logger = logging.getLogger(__name__ + ".FlatInventoryVariantObservation")

    def __init__(self, item_list: Sequence[str], use_variants=False):
        """
        Args:
            item_list: String item identifiers without variant
                suffixes. Every item identifier must be of the form
                "{item_type}#{variant_num}", where variant_num is an integer
                in range(16).
        """
        assert len(item_list) == len(set(item_list)), "duplicate items"
        self.use_variants = use_variants

        # self.item_type_to_variants: Dict[str, Set[int]] = collections.defaultdict(set)
        for item in item_list:
            item_type, variant_num = item.split('#')
            assert isinstance(item_type, str)
            assert int(variant_num) in range(16)
            # self.item_type_to_variants[item_type].add(variant_num)

        # self.item_type_to_variants = dict(self.item_type_to_variants)

    def from_hero(self, info):
        """
        Converts the Hero observation into a one-hot of the inventory items
        for a given inventory container. Ignores variant / color
        :param obs:
        :return:
        """
        item_dict = self.space.no_op()
        for stack in info['inventory']:
            # Requires that every item type returned contains '#'.
            variant_item_name = _get_variant_item_name(stack['type'], stack['variant'])
            type_with_variant = stack['type']
            if '#' not in type_with_variant:
                raise ValueError(
                    f"Expected stack['type']={type_with_variant} to contain '#'")
            type_name, variant = type_with_variant.split('#')

            if self.use_variants:
                key = type_with_variant
            else:
                key = type_name

            if key in item_dict.keys():
                item_dict[key] += stack['quantity']
        return item_dict

    def from_universal(self, obs):
        item_dict = self.space.no_op()
        slots = _univ_obs_get_inventory_slots(obs)
        for stack in slots:
            # for key in ['type', 'variant', 'quantity']:
            #     if key not in stack:
            #         continue
            type_name = mc.strip_item_prefix(stack['name'])
            assert type_name != stack['name']  # Just making sure...
            variant = stack['variant']
            quantity = stack['quantity']
            if self.use_variants:
                key = f"{type_name}#{variant}"
            else:
                key = type_name
            if key in item_dict:
                item_dict[key] += quantity
        return item_dict
