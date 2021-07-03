# Installation test
import minerl_patched
import time
from minerl_patched.env.bootstrap import MinecraftInstance


def main():
    """
    Tests launching and closing a Minecraft process.
    We should use the python unit test framework :O
    """
    inst = MinecraftInstance()
    inst.launch(9000)
    time.sleep(10)
    inst.kill()


if __name__ == "__main__":
    main()
