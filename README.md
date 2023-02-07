# SwiftTalk
SwiftTalk is a java program made to communicate with *SWIFT Alliance Access* from SWIFT company (https://swift.com).

SwiftTalk is able to send SWIFT messages to SAA by :
- scanning a folder for XML files containing SWIFT messages in SAA XML v2 format
- sending the file content to a IBM MQ queue
- archiving files successfully sent in a dedicated folder

SwiftTalk is able to receive messages from SAA by :
- scanning a IBM MQ queues for messages in SAA XML v2 format
- detect the kind of message retrieved from the queues (PAN/NAN/ACK/NACK/MT/MX)
- create files containing SWIFT messages, sorted by sender and message type

LAU will be implemented in a near future.

## Configuration
SwiftTalk configuration is managed in a file named SwiftTalk.properties
The following properties should be set :
- FOLDER_TO_SWIFT : folder to be scanned to sent SWIFT messages to SAA
- ARCHIVE_FOLDER : folder where successfully sent files are moved
- RETENTION_PERIOD : number of days to keep archived files (older files are removed when starting SwiftTalk)
- FOLDER_FROM_SWIFT : folder where files containing SWIFT messages received from SAA are created
- QMGRHOST : hostname where the IBM MQ Manager is hosted
- QMGRNAME : name of the queue manager
- QMGRPORT : port number used by the MQ listener
- CHANNEL : channel name to communicate with the queue manager
- QUEUE_TO_SWIFT : queue used to send SWIFT messages to SAA
- REPLY_TO_QUEUE : queue used to receive SAA PAN / NAN
- QUEUE_ACK_SWIFT : queue used to receive SWIFT ACK/NACK
- QUEUE_FROM_SWIFT : queue used to receive SWIFT messages from SAA
- SLEEPING_DURATION : sleeping duration in seconds, used when there is no more file to send and all queues have been scanned without any message

The REPLY_TO_QUEUE, QUEUE_ACK_SWIFT and QUEUE_FROM_SWIFT could be the same. SwiftTalk is able to detect what kind of message has been received and process it accordingly.