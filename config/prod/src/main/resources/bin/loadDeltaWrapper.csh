#!/bin/csh -f
#
# Sample cron entry:
# Minute     Hour     Day of Month     Month     Day of Week
# 0      19      *       *       0       csh /home/mapping-rest/config/bin/loadDeltaWrapper.csh > /home/mapping-rest/logs/loadDelta.log
#
# This script is controlled by a switch
# The switch is on by default.  To turn it off
# Create a file called "OFF" in /home/mapping-rest/bin
#

# Check the switch
echo "Check the switch"
if (-e /home/mapping-rest/bin/OFF) then
        echo "The switch is turned off, don't run"
        exit 1
endif

# enable maintenance page
/opt/maint/getMaintHtml.sh start

# Runs load delta and ensures server is up
/home/mapping-rest/config/bin/loadDelta.csh
set x = $status
if ($x != 0) then
    supervisorctl start mapping-rest
    return $x
endif

# disable maintenance page
/opt/maint/getMaintHtml.sh stop
