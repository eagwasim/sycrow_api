import getopt
import os
import sys
from shutil import copyfile

# Get the arguments from the command-line except the filename
argv = sys.argv[1:]

mappings = {
    'live': {
    },
    'staging': {
    }
}

project_names = {
    'live': 'sycrow-api',
    'staging': ''
}


def _move_files(mapping, project_name):
    for file_1 in mapping:
        copyfile(file_1, mapping[file_1])

    os.system('gcloud config set project %s' % project_name)
    os.system('mvn clean')
    # if project_name == "kalori-live":
    #   os.system('git checkout master')
    # elif project_name == "kalori-staging-d9a0c":
    #   os.system('git checkout stage')
    os.system('gcloud app deploy pom.xml')


try:
    # Define the getopt parameters
    opts, args = getopt.getopt(argv, 'd:', ['deploy'])
    # Check if the options' length is 2 (can be enhanced)
    if len(opts) == 0 or len(opts) > 1:
        print('usage: alice.py -d <live or staging>')
    else:
        # Iterate the options and get the corresponding values
        mode = None;
        for opt, arg in opts:
            mode = arg
            break
        if mode != 'live' and mode != 'staging':
            print('usage: alice.py -d <live or staging>')
        else:
            _move_files(mappings[mode], project_names[mode])

except getopt.GetoptError:
    # Print something useful
    print('usage: alice.py -d <live or staging>')
    sys.exit(2)
