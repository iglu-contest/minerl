import os
import shutil
import minerl_patched

MINERL_ROOT = os.environ.get('MINERL_DATA_ROOT')

if os.path.exists(MINERL_ROOT):
    shutil.rmtree(MINERL_ROOT)

minerl_patched.data.download(minimal=True)
