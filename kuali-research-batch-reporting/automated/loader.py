import sys
import os
import debugpy
import shutil

def start():
  # We are in a docker container, so we cannot do this: debugpy.listen(5678)
  debugpy.listen(("0.0.0.0", 5678))
  print("Waiting for debugger attach")
  debugpy.wait_for_client()
  debugpy.breakpoint()
  print('Will break on this line')

  report_cd = sys.argv[1]

  print("report_cd: {rcd}".format(rcd=report_cd))

  # Won't work until v3.8: shutil.copytree("./code/{rcd}".format(rcd=report_cd), '.', dirs_exist_ok=True)
  srcdir = "./code/{rcd}".format(rcd=report_cd)
  files_to_copy = os.listdir(srcdir)
  for fname in files_to_copy:
    shutil.copy2(os.path.join(srcdir, fname), '.')
  print('Contents of /automated:')
  print(os.listdir('.'))

  # This will execute the copied module since all of it's code is at the global level
  start = __import__('_main', globals(), locals(), [], 0)

  print("Finished debug running {rcd}".format(rcd=report_cd))

start()