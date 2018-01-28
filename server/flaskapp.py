#!/usr/bin/env python
# -*- coding: utf-8 -*-

from flask import Flask
import json
import os
from flask import request, jsonify, render_template
import pymysql
import sys

app = Flask(__name__)

db_name = 'ardraw_db'
name = 'ardraw_user'
password = ''
rds_host = 'ardraw-db-instance.crr5w2xqdwfp.us-east-2.rds.amazonaws.com'
port = 3306

try:
	conn = pymysql.connect(rds_host, user=name, passwd=password, db=db_name, connect_timeout=5)
except:
	print("ERROR: Could not connect to database instance.")
	sys.exit()



@app.route("/")
def hello():
    return 'Hello World'

if __name__ == "__main__":
    app.run()

