from flask import Flask
app = Flask(__name__)
import json
import os
from flask import request, jsonify, render_template

'''
####### ENVIRONMENT VARIABLES #######

RDS_HOSTNAME – The hostname of the DB instance. ("Endpoint" in AWS)
RDS_PORT – The port on which the DB instance accepts connections. The default value varies between DB engines. ("Port" in AWS)
RDS_DB_NAME – The database name, ebdb. ("DB Name" on AWS)
RDS_USERNAME – The user name that you configured for your database. ("Username" in AWS)
RDS_PASSWORD – The password that you configured for your database.
'''
# $ export FLASK_APP=flaskapp.py
# $ flask run

if 'RDS_HOSTNAME' in os.environ:
    DATABASES = {
        'default': {
            'ENGINE': 'django.db.backends.mysql',
            'NAME': os.environ['RDS_DB_NAME'],
            'USER': os.environ['RDS_USERNAME'],
            'PASSWORD': os.environ['RDS_PASSWORD'],
            'HOST': os.environ['RDS_HOSTNAME'],
            'PORT': os.environ['RDS_PORT'],
        }
    }

@app.route("/")
def hello():
    return 'Hello World'

if __name__ == "__main__":
    app.run()

