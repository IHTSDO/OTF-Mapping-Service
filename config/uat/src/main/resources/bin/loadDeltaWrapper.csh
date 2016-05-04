#!/bin/csh -f
#
# Sample cron entry:
# Minute     Hour     Day of Month     Month     Day of Week
# 0      19      *       *       0       csh /home/ihtsdo/config/bin/loadDeltaWrapper.csh > /home/ihtsdo/logs/loadDelta.log
#
# This script is controlled by a switch
# The switch is on by default.  To turn it off
# Create a file called "OFF" in /home/ihtsdo/bin
#

# Check the switch
echo "Check the switch"
if (-e /home/ihtsdo/bin/OFF) then
        echo "The switch is turned off, don't run"
        exit 1
endif

# Runs load delta and ensures server is up
/home/ihtsdo/bin/loadDelta.csh
set x = $status
if ($x != 0) then
    service tomcat7 start
    return $x
endif
