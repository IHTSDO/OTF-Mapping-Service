#!/bin/csh -f

set GMDN_DIR = /home/ihtsdo/data/GMDN
if (! -e $GMDN_DIR) then
    echo "ERROR: $GMDN_DIR does not exist"
    exit 1
endif

echo "----------------------------------------------------------------"
echo "Starting ... `/bin/date`"
echo "----------------------------------------------------------------"

/bin/rm -f /tmp/a$$.txt
sudo aws s3 ls mapping.backup.ihtsdo >&! /tmp/a$$.txt
if ($status != 0) then
    echo "ERROR: failed sudo aws s3 ls failed"
    exit 1
endif

if (`grep gmdn /tmp/a$$.txt | wc -l` > 0) then
    grep gmdn /tmp/a$$.txt | perl -pe 's/.* //' | sort -u -o /tmp/b$$.txt
else
    echo "NO GMDN FILES, bailing"
    /bin/rm -rf /tmp/a$$.txt /tmp/b$$.txt
    exit 0
endif

cd $GMDN_DIR
set ct = 0
foreach f (`cat /tmp/b$$.txt`)
    set dir = `echo $f | sed 's/.zip//' | perl -pe 's/gmdn.*(\d\d_\d\d)/$1/;'`
    if (-e $dir) then
      continue
    endif
    echo "  Processing $dir ...`/bin/date`"
    @ ct = $ct + 1
    sudo aws s3 cp s3://mapping.backup.ihtsdo/$f . | sed 's/^/      /'
    mkdir $dir
    unzip $f -d $dir | sed 's/^/      /'
    /bin/rm -f $f
end

if ($ct > 1) then
    echo "ERROR: Multiple new GMDN versions, must resolve manually"
    exit 1
endif

echo "----------------------------------------------------------------"
echo "Finished ... `/bin/date`"
echo "----------------------------------------------------------------"
