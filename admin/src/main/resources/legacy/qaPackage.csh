#!/bin/csh -f
#
# Validates a prior version RF2 data set against current version
#
# * Verify that the delta has unique "id"
# ** for each .txt file in the $packageDir/RF2Release/Delta folder, verify the first column is unique
# ** e.g. find pubDir/SnomedCT_Icd9CrossMap_INT_20130131/RF2Release/Snapshot/ -name "*txt"
# * Verify that the delta effectiveTime is set only to the current version
# * Verify that the diff between the current full and prior full exactly matches the delta
# * Verify that any inactivation (active=0) in the delta corresponds to an an active entry in the prior version snapshot  (by id)
# * Verify that the snapshot has unique "id"
# * Verify that the entries with effectiveTime set to the current version exactly match the delta
# * Verify that the entries in the snapshot are also in the full
# * Verify that the active portion of the snapshot file passes qaCrossMaps QA
#

#
# Set environment (if configured)
#
if ($?ENV_HOME == 0) then
    echo '$'"ENV_HOME must be set"
    exit 1
endif
if ($?ENV_FILE == 0) then
    echo '$'"ENV_HOME must be set"
    exit 1
endif
source $ENV_HOME/bin/env.csh

#
# Check Environment Variables
#
if ($?PATH_TO_PERL == 0) then
    echo '$PATH_TO_PERL must be set'
    exit 1
endif

#
# Parse arguments
#
set quick = 0
set i=1
while ($i <= $#argv)
    switch ($argv[$i])
        case '-*help':
            cat << EOF
 This script has the following usage:
   Usage: $0 [-[-]help] <prev version RF2> <current version RF2>

   This script performs QA checks on RF2 data sets.

 Options:
    --help                    : display this help

EOF
            exit 0

        default :
            set arg_count=2
            set all_args=`expr $i + $arg_count - 1`
            echo "$arg_count $all_args"
            if ($all_args != $#argv) then
                echo "Error: Incorrect number of arguments: $all_args, $#argv"
                echo "Usage: $0 [-[-]help] [-q] [-namespace <namespaceId>]* [-origin <orignNamespaceId>]
                echo "       [-ext <extDir>] [-core <coreDir>]
                exit 1
            endif
            set prevDir = $argv[$i]
            set i=`expr $i + 1`
            set dir = $argv[$i]
            set i=`expr $i + 1`
    endsw
    set i=`expr $i + 1`
end

echo "----------------------------------------------------------------------"
echo "Starting ... `/bin/date`"
echo "----------------------------------------------------------------------"
echo "Prev dir:      $prevDir"
echo "dir:           $dir"


#
# Verify that the delta has unique "id"
#   for each .txt file in the $packageDir/RF2Release/Delta folder, verify the first column is unique
#   e.g. find pubDir/SnomedCT_Icd9CrossMap_INT_20130131/RF2Release/Snapshot/ -name "*txt"
#
echo "    Verify that the delta has unique id ...`/bin/date`"
foreach f (`find $dir/Delta -name "*txt"`)
    echo "      $f"
    set ct = `cut -f 1 $f | sort | uniq -d | wc -l`
    if ($ct != 0) then
        echo "ERROR: $f has non-unique ids"
        cut -f 1 $f | sort | uniq -d | sed 's/^/      /'
    endif
end

#
# Verify that the delta effectiveTime is greater than the previous version
#
echo "    Verify that the delta effectiveTime is greater than the previous version ...`/bin/date`"
set prevVersion = `ls $prevDir/Delta/Refset/Map/*Map*txt | $PATH_TO_PERL -pe 's/.*_(\d{8}).txt/$1/'`
foreach f (`find $dir/Delta -name "*txt"`)
    echo "      $f"
    set ct = `cut -f 2 $f | grep -v effectiveTime | grep -v alternateId | $PATH_TO_PERL -ne 'chop; print if $_ le "'$prevVersion'"' | wc -l`
    if ($ct != 0) then
        echo "ERROR: $f has effectiveTime not matching $version"
        cut -f 2 $f | grep -v effectiveTime | grep -v alternateId | $PATH_TO_PERL -ne 'chop; print if $_ le "'$prevVersion'"' | sed 's/^/      /'
    endif
end

#
# Verify that the diff between the current full and prior full exactly matches the delta
#
echo "    Verify that the diff between the current full and prior full exactly matches the delta ...`/bin/date`"
set prevVersion = `ls $prevDir/Delta/Refset/Map/*Map*txt | $PATH_TO_PERL -pe 's/.*_(\d{8}).txt/$1/'`
set version = `ls $dir/Delta/Refset/Map/*Map*txt | $PATH_TO_PERL -pe 's/.*_(\d{8}).txt/$1/'`
foreach f (`find $dir/Full -name "*txt"`)
    echo "      $f"
    set prevF = `echo $f | sed "s/$version.txt/$prevVersion.txt/g; s%$dir%$prevDir%;"`
    echo "      $prevF"
    # Map was renamed to Map, try to find it this way
    if (! -e $prevF) then
        set prevF = `echo $f | sed "s/$version.txt/$prevVersion.txt/g; s%$dir%$prevDir%; s%/Map/%/Map/%g;"`
    endif
    set deltaF = `echo $f | sed "s/Full/Delta/g;"`
    echo "      $deltaF"
    if (! -e $prevF) then
        echo "ERROR: Unable to find prev version file"
        echo "       -> $prevF"
    else
        diff $f $prevF >&! diff.$$.txt
        if (`egrep -c '^>' diff.$$.txt` > 0) then
            echo "ERROR: prev file contains entries that current file does not"
            echo "      -> $prevF"
            egrep  '^>' diff.$$.txt
        endif
        grep '<' diff.$$.txt | sed 's/< //' | sort -u -o delta.$$.txt
        if (! -e $deltaF) then
            echo "ERROR: Unable to find delta file"
            echo "       -> $deltaF"
        else
            $PATH_TO_PERL -e '<>; while (<>) { print; }' $deltaF | sort -u -o deltaOrig.$$.txt
            if (`diff delta.$$.txt deltaOrig.$$.txt | wc -l` > 0) then
                echo "ERROR: diff between current and prev files does not match the delta"
                diff delta.$$.txt deltaOrig.$$.txt | sed 's/^/      /'
            endif
         endif
        /bin/rm -f diff.$$.txt delta.$$.txt deltaOrig.$$.txt
    endif
end

#
# Verify that any inactivation (active=0) in the delta corresponds to an an active entry in a prior version full entry (by id)
#
echo "    Verify that any inactivation (active=0) in the delta corresponds to an an active entry in a prior version full entry ...`/bin/date`"
set prevVersion = `ls $prevDir/Delta/Refset/Map/*Map*txt | $PATH_TO_PERL -pe 's/.*_(\d{8}).txt/$1/'`
set version = `ls $dir/Delta/Refset/Map/*Map*txt | $PATH_TO_PERL -pe 's/.*_(\d{8}).txt/$1/'`
foreach f (`find $dir/Delta -name "*txt"`)
    echo "      $f"
    set prevF = `echo $f | sed "s/$version.txt/$prevVersion.txt/g; s%$dir%$prevDir%; s/Delta/Full/g;"`
    # Map was renamed to Map, try to find it this way
    if (! -e $prevF) then
        set prevF = `echo $f | sed "s/$version.txt/$prevVersion.txt/g; s%$dir%$prevDir%; s/Delta/Full/g;"`
    endif
    echo "      $prevF"
    $PATH_TO_PERL -ne 'split/\t/; print if $_[2] eq "0"' $f | cut -f 1 | sort -u -o inactiveDelta.$$.txt
    $PATH_TO_PERL -ne 'split/\t/; print if $_[2] eq "1"' $prevF | cut -f 1 | sort -u -o activeFull.$$.txt
    set ct = `comm -23 inactiveDelta.$$.txt activeFull.$$.txt | wc -l`
    if ($ct != 0) then
        echo "ERROR: inactive delta entries without prior version active full entries"
        comm -23 inactiveDelta.$$.txt activeFull.$$.txt | sed 's/^/      /'
    endif
    /bin/rm -f inactiveDelta.$$.txt activeFull.$$.txt
end

#
# Verify that the snapshot has unique "id"
#
echo "    Verify that the snapshot has unique id ...`/bin/date`"
foreach f (`find $dir/Snapshot -name "*txt"`)
    echo "      $f"
    set ct = `cut -f 1 $f | sort | uniq -d | wc -l`
    if ($ct != 0) then
        echo "ERROR: $f has non-unique ids"
        cut -f 1 $f | sort | uniq -d | sed 's/^/      /'
    endif
end

#
# Verify that snapshot entries with effectiveTime set later than previous version exactly match the delta
#
echo "    Verify that snapshot entries with effectiveTime set later than previous version exactly match the delta ...`/bin/date`"
set prevVersion = `ls $prevDir/Delta/Refset/Map/*Map*txt | $PATH_TO_PERL -pe 's/.*_(\d{8}).txt/$1/'`
foreach f (`find $dir/Snapshot -name "*txt"`)
    echo "      $f"
    set deltaF = `echo $f | sed "s/Snapshot/Delta/g;"`
    echo "      $deltaF"
    $PATH_TO_PERL -ne 'split /\t/; print if $_[1] gt "'$prevVersion'" && $_[1] !~ /[a-z]/' $f | sort -u -o currentSnap.$$.txt
    $PATH_TO_PERL -e '<>; while (<>) { print; } ' $deltaF | sort -u -o delta.$$.txt
    set ct = `diff currentSnap.$$.txt delta.$$.txt | wc -l`
    if ($ct != 0) then
        echo "ERROR: current version snapshot entries do not match the delta"
        diff currentSnap.$$.txt delta.$$.txt | sed 's/^/      /'
    endif
    /bin/rm -f currentSnap.$$.txt delta.$$.txt
end

#
# Verify that the entries in the snapshot are also in the full
#
echo "    Verify that the entries in the snapshot are also in the full ...`/bin/date`"
foreach f (`find $dir/Snapshot -name "*txt"`)
    echo "      $f"
    set fullF = `echo $f | sed "s/Snapshot/Full/g; "`
    echo "      $fullF"
    sort -u -o snap.$$.txt $f
    sort -u -o full.$$.txt $fullF
    set ct = `comm -23 snap.$$.txt full.$$.txt | wc -l`
    if ($ct != 0) then
        echo "ERROR: entries in snapshot file not in full"
        comm -23 snap.$$.txt full.$$.txt | sed 's/^/      /'
    endif
    /bin/rm -f snap.$$.txt full.$$.txt
end

echo "----------------------------------------------------------------------"
echo "Finished ... `/bin/date`"
echo "----------------------------------------------------------------------"
