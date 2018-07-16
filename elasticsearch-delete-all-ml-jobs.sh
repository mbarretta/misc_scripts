#!/bin/bash
HOST='localhost'
PORT=9200
CURL_AUTH="-u elastic:changeme"

echo
echo
list=`curl $CURL_AUTH -s http://$HOST:$PORT/_xpack/ml/anomaly_detectors?pretty | awk -F" : " '/job_id/{print $2}' | sed 's/\",//g' | sed 's/\"//g'`
while read -r JOB_ID; do
   echo
   echo "Deleting  ${JOB_ID}'s datafeed..."
   curl $CURL_AUTH -s -XDELETE $HOST:$PORT/_xpack/ml/datafeeds/datafeed-${JOB_ID}
   echo "Deleting  ${JOB_ID}..."
   curl $CURL_AUTH -s -XDELETE $HOST:$PORT/_xpack/ml/anomaly_detectors/${JOB_ID}

   echo
   echo
   echo "-------------"
   echo

done <<< "$list"
