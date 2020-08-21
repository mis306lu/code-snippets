import json
import sys
import requests
import psycopg2
import time
import urllib.request, json 
from collections import OrderedDict
from pymarc import JSONReader, Field, JSONWriter, XMLWriter
import time
import logging
from datetime import date, timedelta, datetime

#####NOTES
#THE FILE NAME & PATH WITH THE OCLC NUMBERS IS HARDCODED (LINE 49)
#OUR IDENTIFIER TYPE UUID IS HARDCODED (LINE 56)
#OUR OKAPI ENDPOINT, TENANT, USERID, PASSWORD HARDCODED (BELOW)
#YOU MAy HAVE TO CLEAN UP THE SPACING OF THIS FILE
#IT RUNS FOR ME...BUT UPLOADED INTO GIT...LOOKS OFF

#USED TO CREATE A FILE NAME BASED ON A TIMESTAMP
timestr = time.strftime("%Y%m%d-%H%M%S")

url = "https://lehigh-okapi.folio.indexdata.com"
#url = "https://folio-test.lib.lehigh.edu/okapi"
tenant = "lu"


headers = {"x-okapi-tenant": tenant, "Content-type": "application/json"}

#AUTHENTICATE
user = {}
user['username'] = "----"
user['password'] = "----"
user['tenant'] = tenant
the_data = json.dumps(user)
response = requests.post(url + "/authn/login",the_data,headers=headers)
token = response.headers['x-okapi-token']


headers = {"x-okapi-tenant": tenant, "Content-type": "application/json","x-okapi-token":token}

#get holdings type electronic / UUID - for query below
query = url + "/holdings-types?query=(name==Electronic)&limit=1";
print(query)
holdingsTypeResponse = requests.get(query,headers=headers)
print(holdingsTypeResponse)
the_data = holdingsTypeResponse.json()
holdingsTypeUuid = the_data["holdingsTypes"][0]["id"]

writer = open("outfile-" + timestr + ".mrc",'wb')
f = open("c:/users/mis306/desktop/oclcs_wiley_ucbm.txt", "r")
#FOR EACH OCLC NUMBER IN THE FILE
#EXAMPLE ENTRY: (OCoLC)982487176
for x in f:
	oclc = x.strip(' \t\n\r');
	print(x);
	#LOOKUP OCLC NUMBER IN FOLIO
	query = url + '/inventory/instances?limit=100&query=(identifiers =/@value/@identifierTypeId="439bfbae-75bc-4f74-9fc7-b2a2d47ce3ef" "' + oclc + '")'
	#TESTING VUFIND QUERY
	#query = "vufind-lehigh.edu/api/v1/search?lookfor=" + oclc + "&type=oclc_num"
	print(query);
	response = requests.get(query,headers=headers)
	#print(response)
	the_data = response.json()
	#FOR EACH INSTANCE FOUND THAT CONTAINED THE OCLC NUMBER
	for v in the_data['instances']:
		print(v['id'])
		instanceUuid = v['id']
		#GET THE MARC RECORD
		marcResponse = requests.get(url + "/source-storage/records/" + instanceUuid + "/formatted?idType=INSTANCE",headers=headers)
		print(marcResponse)
		marc_data = marcResponse.json()
		marcJsonAsString = marc_data['parsedRecord']['content']
		staffOnly = marc_data['additionalInfo']['suppressDiscovery']
		m = json.dumps(marcJsonAsString) 
		#GET ALL OF HRIDS OF ELECTRONIC HOLDINGS, IF THERE ARE NONE, CONTINUE
		holdingResponse = requests.get(url + "/holdings-storage/holdings?query=(instanceId==" + instanceUuid + " and holdingsTypeId==" + holdingsTypeUuid + ")",headers=headers)
		holdings_data = holdingResponse.json()
		if (len(holdings_data['holdingsRecords']) < 1):
			continue
		#ADDING THE STAFF ONLY INDICATOR OF THE INSTANCE
		#IN THE 949
		for record in JSONReader(m):
			record.add_field(Field(tag = '949',
										indicators = ['',''],
										subfields = ['x',str(staffOnly)]))
			#ADDING THE HOLDINGHrid, uri, StaffOnly to 852
			#FOR EACH OF THE ELECTRONIC HOLDINGS
			fieldNumber = 852;
			for h in holdings_data['holdingsRecords']:
				holdingHrid = h["hrid"];
				holdingDiscoverySuppress = h.get('discoverySuppress','false')

				try:
					uri = h['electronicAccess'][0]['uri']
				except KeyError:
					uri = ''
				
				record.add_field(Field(tag = str(fieldNumber),
										indicators = ['',''],
										subfields = ['a',holdingHrid,
													 'u',uri,
													 'x',str(holdingDiscoverySuppress),
													 ]))
				
				fieldNumber = fieldNumber + 1

		print(record)
		writer.write(record.as_marc())

writer.close()
			
