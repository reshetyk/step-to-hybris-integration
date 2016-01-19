call java -jar build/libs/step-to-hybris-integration-all-1.0.jar^
 "tcp://ALREWIN7:61616"^
 "sftp://stibosw@hyb-commerce:22//tmp/test-hot-folder?fileName=${exchangeId}.impex&password=stibosw&username=stibosw&stepwise=false&disconnect=true"^
 "smtps://reshetyk@gmail.com:465?host=smtp.gmail.com&password=TestPassword&username=aleksey.reshetuyk.2test"