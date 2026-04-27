SET SPTARGET=sandbox
### The below executable jar is used to generate env.include.properties files used by the ant script to build subset
### of deployment artifacts
##java -jar admintools.jar org.ascn.admintools.BuildIncludeFile  "target" "todaydate" "workkingDir"
##java -jar admintools.jar org.ascn.admintools.BuildIncludeFile  "dev" "MMddyyyy" "<Root Path of the SSB Folder>"
## todayDate = Get-Date -UFormat %m%d%Y;
##Example: java -jar admintools.jar org.ascn.admintools.BuildIncludeFile  "%SPTARGET%" "todayDate" "D:\\IdentityIIQ-Projects\\"
ant clean main import-custom-subset