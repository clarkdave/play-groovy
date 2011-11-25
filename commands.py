# groovy
import sys
import inspect
import os
import subprocess
import shutil

from play.utils import *

MODULE = 'groovy'

COMMANDS = ['groovy:console']

def execute(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    env = kargs.get("env")

    if command == "groovy:console":
        # add precompiled classes to classpath
        cp_args = app.cp_args() + ":" + os.path.normpath(os.path.join(app.path,'tmp', 'classes'))
        # replace last element with the console app
        java_cmd = app.java_cmd(args, cp_args)
        java_cmd[len(java_cmd)-2] = "play.console.Console"
        java_cmd.insert(2, '-Xmx512M')
        subprocess.call(java_cmd, env=os.environ)
        print


def before(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    env = kargs.get("env")


def after(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    env = kargs.get("env")

    if command == "new":
        #shutil.rmtree(os.path.join(app.path, 'app/controllers'))
        #shutil.rmtree(os.path.join(app.path, 'app/models'))
        #shutil.rmtree(os.path.join(app.path, 'app/views/errors'))
        os.remove(os.path.join(app.path, 'app/controllers/Application.java'))
        os.remove(os.path.join(app.path, 'test/BasicTest.java'))
        os.remove(os.path.join(app.path, 'test/ApplicationTest.java'))
        module_dir = inspect.getfile(inspect.currentframe()).replace("commands.py", "")
        shutil.copyfile(os.path.join(module_dir, 'resources', 'Application.groovy'), os.path.join(app.path, 'app/controllers', 'Application.groovy'))
        shutil.copyfile(os.path.join(module_dir, 'resources', 'Tests.groovy'), os.path.join(app.path, 'test', 'Tests.groovy'))
        ac = open(os.path.join(app.path, 'conf/application.conf'), 'r')
        conf = ac.read()
        ac = open(os.path.join(app.path, 'conf/application.conf'), 'w')
        ac.write(conf)
    
    # TODO: set up eclipsify and stuff
