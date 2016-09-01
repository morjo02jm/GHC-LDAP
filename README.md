# githubldap
Basic process for adding and deleting users from the Github control DL, Development-Tools-Access-Group

Prerequisites:

a) JRE 1.8.0
b) CA Harvest SCM jhsdk.jar and dependent libraries.

Usage:

java -jar githubldap.jar -add [list of users to add to DL] -del [list of users to delete from DL]
                         [-developers | -admins]
                         [-sync]
                         -log [folder to place run log]
