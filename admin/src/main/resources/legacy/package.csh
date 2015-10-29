#!/bin/csh -f
#
# Takes the created publication files and packages them into the .zip format in the specified dir
#

set usage = "Usage: $0 <packageDir>"

#
# Set environment (if configured)
#

#
# Check required environment variables
#
if ($?MAPPING_HOME == 0) then
    echo "MAPPING_HOME must be set"
    exit 1
endif

#
# Parse arguments
#
if ($#argv == 0) then
    echo "$usage"
    exit 1
endif

set i=1

set cleanup = 0
while ($i <= $#argv)
    switch ($argv[$i])
        case '-cleanup':
            set cleanup = 1
            breaksw
        case '-*help':
                cat << EOF
 This script assumes publication has been performed and files are in $MAPPING_HOME/data.
 It also assumes that packageDir is a directory containing the previous version files
 in the structure of a SNOMED release.

 It creates a new package structure that appends to the prior version files and leaves
 a new directory structure with full, snapshot, and delta files with new file naming
 conventions and ensures all resources are copied over and updated as needed.

  $usage

 Parameters:
    <prevPackageDir> : The directory of the unzipped artifact from the previous release
                       (note, this was "faked" for the baseline release
 Options:
    -cleanup         : Delete the new package structure before starting
    -[-]help         : On-line help

EOF
            exit 0
        default :
            set arg_count=1
            set all_args=`expr $i + $arg_count - 1`
            if ($all_args != $#argv) then
                echo "Error: Incorrect number of arguments: $all_args, $#argv"
                echo "$usage"
                exit 1
            endif
            set prevPackageDir=$argv[$i]
            @ i = $i + 1
    endsw
    @ i = $i + 1
end

#
# Begin program logic
#
echo "----------------------------------------------"
echo "Starting $0 ... `/bin/date`"
echo "----------------------------------------------"
echo "MAPPING_HOME:   $MAPPING_HOME"
echo "prevPackageDir: $prevPackageDir"
echo "cleanup flag:   $cleanup"
echo ""
set prevPackageDir = `echo $prevPackageDir | sed 's/\/$//'`

#
# Verify assumptions
#
if (! -e $prevPackageDir) then
    echo "ERROR: $prevPackageDir does not exist"
    exit 1
endif

if (! -e "$MAPPING_HOME/data") then
    echo "ERROR: $MAPPING_HOME/data does not exist"
    exit 1
endif

set mapFile = $MAPPING_HOME/data/der2_iisssccRefset*txt >>& /dev/null
if ($status != 0) then
    echo "ERROR: Map file (der2_iissssccRefset*txt) does not exist in"
    echo "        $MAPPING_HOME/data"
    exit 1
endif

set version = `echo $mapFile:t | $PATH_TO_PERL -pe 's/.*(\d{8}).*/$1/'`
if ($status != 0) then
    echo "ERROR: unable to determine version from $mapFile"
    exit 1
endif

set prevVersion = `echo $prevPackageDir | $PATH_TO_PERL -pe 's/.*(\d{8})$/$1/'`
if ($status != 0) then
    echo "ERROR: unable to determine version from $prevPackageDir"
    exit 1
endif

#
# Print some config info
#
echo "    prev version = $prevVersion"
echo "    version = $version"

set packageDir = "$prevPackageDir:h/SnomedCT_Icd10CrossMap_INT_$version"
echo "    prev package dir = $prevPackageDir"
echo "    package dir = $packageDir"

if ($packageDir == $prevPackageDir) then
    echo "ERROR: prev and current package dirs are the same"
    exit 1
endif

#
# Check for existance of $packageDir, cleanup if desired
#
if (-e $packageDir) then
    if ($cleanup) then
        /bin/rm -rf $packageDir
        /bin/rm -f $packageDir:h/$packageDir:t.zip
    else
        echo "ERROR: $packageDir already exists, clean it up first"
        exit 1
    endif
endif

#
# Prep $packageDir directories and intial files
#
echo "    Prep $packageDir ... `/bin/date`"
mkdir $packageDir
pushd $prevPackageDir >>& /dev/null
zip -r x$$.zip * >>& /dev/null
popd >>& /dev/null
mv $prevPackageDir/x$$.zip $packageDir
pushd $packageDir >>& /dev/null
unzip x$$.zip >>& /dev/null
/bin/rm -f x$$.zip
popd >>& /dev/null

#
# Rename all files from $prevVersion to $version
#
pushd $packageDir >>& /dev/null
foreach f (`find . -name "*.[a-z]*" | grep -v ' '`)
    set f2 = `echo "$f" | sed "s/$prevVersion/$version/"`
    if ($f != $f2) then
        mv $f $f2
    endif
end
if (`find . -name "*.[a-z]*" | grep ' ' | wc -l` > 0) then
    echo "NOTE: Files with spaces are present, they must be handled manually"
    find . -name "*.[a-z]*" | grep ' ' | sed 's/^/      /'
endif
popd >>& /dev/null

#
# Deploy the .tsv file (the file name should match)
#
echo "    Deploying .tsv file `/bin/date`"
set tsvFile = $MAPPING_HOME/data/tls*_$version.tsv
if ($status != 0) then
    echo "ERROR: $MAPPING_HOME/data/tls*_$version.tsv file missing"
    exit 1
endif
set tsvFileName = $tsvFile:t
if (! -e $packageDir/Documentation/$tsvFileName) then
    echo "ERROR: $tsvFileName does not exist in $packageDir"
    exit 1
endif
cp $tsvFile $packageDir/Documentation/$tsvFileName

#
# Deploy the crossmap file
#
echo "    Deploying der2_iisssccRefset file `/bin/date`"
set mapFile = $MAPPING_HOME/data/der2_iisssccRefset*$version.txt
if ($status != 0) then
    echo "ERROR: $MAPPING_HOME/data/der2_iisssccRefset*$version.txt does not exist"
    exit 1
endif
set mapFileName = $mapFile:t
set deltaMapFileName = `echo $mapFileName | sed 's/Snapshot/Delta/'`
set fullMapFileName = `echo $mapFileName | sed 's/Snapshot/Full/'`

echo "      delta"
if (! -e $packageDir/RF2Release/Delta/Refset/Map/$deltaMapFileName) then
    echo "ERROR: $deltaMapFileName does not exist in $packageDir"
    exit 1
endif
# Delta are the lines from current snapshot not in the previous full
# plus lines from the previous snapshot that are not in the current snapshot
#  converted to be inactive with current version effective times
cut -f 1,3- $mapFile | sort -u -o x$$.txt
cut -f 1,3- $packageDir/RF2Release/Snapshot/Refset/Map/$mapFileName |\
    $PATH_TO_PERL -ne 'split /\t/; print if $_[1] == 1' | sort -u -o y$$.txt
comm -23 x$$.txt y$$.txt | cut -f 1 | sort -u -o new$$.txt
comm -23 y$$.txt x$$.txt | cut -f 1 | comm -23 - new$$.txt | sort -u -o retired$$.txt
sort -k 1,1 $mapFile |\
    join -t\     -j1 1 -o 1.1 1.2 1.3 1.4 1.5 1.6 1.7 1.8 1.9 1.10 1.11 1.12 1.13 - new$$.txt | sort -u -o z$$.txt
sort -k 1,1 $packageDir/RF2Release/Snapshot/Refset/Map/$mapFileName |\
join -t\         -j1 1 -o 1.1 1.2 1.3 1.4 1.5 1.6 1.7 1.8 1.9 1.10 1.11 1.12 1.13 - retired$$.txt |\
    $PATH_TO_PERL -ne 'split /\t/; $_[2] = "0"; $_[1] = "'$version'"; print join "\t", @_' |\
    sort -u >> z$$.txt

head -1 $mapFile >! $packageDir/RF2Release/Delta/Refset/Map/$deltaMapFileName
grep -v referencedComponentId z$$.txt |\
sort -t\         -k 5,5 -k 6,6n -k 7,7n -k 8,8n -k 1,4 -k 9,9 -k 10,10 -k 11,11 -k 12,12 -k 13,13 \
   >> $packageDir/RF2Release/Delta/Refset/Map/$deltaMapFileName
/bin/rm -rf [xyz]$$.txt new$$.txt retired$$.txt

echo "      full"
if (! -e $packageDir/RF2Release/Full/Refset/Map/$fullMapFileName) then
    echo "ERROR: $fullMapFileName does not exist in $packageDir"
    exit 1
endif
# Full are the lines from the delta added to the previous full
head -1 $packageDir/RF2Release/Full/Refset/Map/$fullMapFileName >! x$$.txt
sort -u $packageDir/RF2Release/Full/Refset/Map/$fullMapFileName \
        $packageDir/RF2Release/Delta/Refset/Map/$deltaMapFileName |\
    sort -t\     -k 5,5 -k 6,6n -k 7,7n -k 8,8n -k 1,4 -k 9,9 -k 10,10 -k 11,11 -k 12,12 -k 13,13 |\
    grep -v referencedComponentId >> x$$.txt
mv x$$.txt $packageDir/RF2Release/Full/Refset/Map/$fullMapFileName

echo "      snapshot"
# Snapshot are the latest states of each ID from the full
head -1 $packageDir/RF2Release/Full/Refset/Map/$fullMapFileName >! x$$.txt
$PATH_TO_PERL -e ' \
    open($IN,$ARGV[0]) || die "Could not open $ARGV[0]: $! $?\n"; \
    while (<$IN>) { \
        ($id, $et, @x) = split /\t/; \
        next if $id =~ /^id/; \
        if (! $max{$id} || $max{$id} le $et) { \
            $max{$id} = $et; $lines{$id} = $_; \
        } \
    } \
    close($IN); \
    foreach $line (keys %lines) { \
        print $lines{$line}; \
    } ' $packageDir/RF2Release/Full/Refset/Map/$fullMapFileName |\
    sort -t\     -k 5,5 -k 6,6n -k 7,7n -k 8,8n -k 1,4 -k 9,9 -k 10,10 -k 11,11 -k 12,12 -k 13,13 |\
    grep -v referencedComponentId >> x$$.txt
mv x$$.txt $packageDir/RF2Release/Snapshot/Refset/Map/$mapFileName


#
# Deploy the module dependency smap file
#
echo "    Deploying der2_ssRefset file `/bin/date`"
set mdFile = $MAPPING_HOME/data/der2_ssRefset*$version.txt
if ($status != 0) then
    echo "ERROR: $MAPPING_HOME/data/der2_ssRefset*$version.txt does not exist"
    exit 1
endif
set mdFileName = $mdFile:t
set deltaMdFileName = `echo $mdFileName | sed 's/Snapshot/Delta/'`
set fullMdFileName = `echo $mdFileName | sed 's/Snapshot/Full/'`


echo "      delta"
if (! -e $packageDir/RF2Release/Delta/Refset/Metadata/$deltaMdFileName) then
    echo "ERROR: $deltaMdFileName does not exist in $packageDir"
    exit 1
endif
# Delta are the lines from current snapshot not in the previous full
# plus lines from the previous snapshot that are not in the current snapshot
#  converted to be inactive with current version effective times
cut -f 1,3- $mdFile | sort -u -o x$$.txt
cut -f 1,3- $packageDir/RF2Release/Snapshot/Refset/Metadata/$mdFileName |\
    $PATH_TO_PERL -ne 'split /\t/; print if $_[1] == 1' | sort -u -o y$$.txt
comm -23 x$$.txt y$$.txt | cut -f 1 | sort -u -o new$$.txt
comm -23 y$$.txt x$$.txt | cut -f 1 | comm -23 - new$$.txt | sort -u -o retired$$.txt
sort -k 1,1 $mdFile |\
    join -t\     -j1 1 -o 1.1 1.2 1.3 1.4 1.5 1.6 1.7 1.8 - new$$.txt | sort -u -o z$$.txt
sort -k 1,1 $packageDir/RF2Release/Snapshot/Refset/Metadata/$mdFileName |\
join -t\         -j1 1 -o 1.1 1.2 1.3 1.4 1.5 1.6 1.7 1.8 - retired$$.txt |\
    $PATH_TO_PERL -ne 'split /\t/; $_[2] = "0"; $_[1] = "'$version'"; print join "\t", @_' |\
    grep -v referencedComponentId |\
    sort -u >> z$$.txt

head -1 $mdFile >! $packageDir/RF2Release/Delta/Refset/Metadata/$deltaMdFileName
grep -v referencedComponentId z$$.txt |\
sort -t\         -k 5,8 -k 1,4 \
   >> $packageDir/RF2Release/Delta/Refset/Metadata/$deltaMdFileName
/bin/rm -rf [xyz]$$.txt new$$.txt retired$$.txt

echo "      full"
if (! -e $packageDir/RF2Release/Full/Refset/Metadata/$fullMdFileName) then
    echo "ERROR: $fullMdFileName does not exist in $packageDir"
    exit 1
endif
# Full are the lines from the delta added to the previous full
head -1 $packageDir/RF2Release/Full/Refset/Metadata/$fullMdFileName >! x$$.txt
sort -u $packageDir/RF2Release/Full/Refset/Metadata/$fullMdFileName \
        $packageDir/RF2Release/Delta/Refset/Metadata/$deltaMdFileName |\
    sort -t\     -k 5,8 -k 1,4 |\
    grep -v referencedComponentId >> x$$.txt
mv x$$.txt $packageDir/RF2Release/Full/Refset/Metadata/$fullMdFileName

echo "      snapshot"
# Snapshot are the latest states of each ID from the full
head -1 $packageDir/RF2Release/Full/Refset/Metadata/$fullMdFileName >! x$$.txt
$PATH_TO_PERL -e ' \
    open($IN,$ARGV[0]) || die "Could not open $ARGV[0]: $! $?\n"; \
    while (<$IN>) { \
        ($id, $et, @x) = split /\t/; \
        next if $id =~ /^id/; \
        if (! $max{$id} || $max{$id} le $et) { \
            $max{$id} = $et; $lines{$id} = $_; \
        } \
    } \
    close($IN); \
    foreach $line (keys %lines) { \
        print $lines{$line}; \
    } ' $packageDir/RF2Release/Full/Refset/Metadata/$fullMdFileName |\
    sort -t\     -k 5,8 -k 1,4 |\
    grep -v referencedComponentId >> x$$.txt
mv x$$.txt $packageDir/RF2Release/Snapshot/Refset/Metadata/$mdFileName

echo "    Set file dates ...`/bin/date`"
touch -t ${version}0000 `find $packageDir -name "*"`
if ($status != 0) then
    echo "ERROR: problem setting file times"
    exit 1
endif

echo "    Cleanup delta content files - they have not changed ...`/bin/date`"
/bin/rm -f $packageDir/RF2Release/Delta/Terminology/*txt
/bin/rm -f $packageDir/RF2Release/Delta/Refset/Content/*txt
/bin/rm -f $packageDir/RF2Release/Delta/Refset/Language/*txt
/bin/rm -f $packageDir/RF2Release/Delta/Refset/Metadata/*RefsetDes*txt
/bin/rm -f $packageDir/RF2Release/Delta/Refset/Metadata/*DescriptionType*txt


echo "    Perform Validation ...`/bin/date`"

#
# Verify no non-Full files in "Full"
#
if (`find $packageDir/RF2Release/Full -name "*txt" | grep -v Full | wc -l` > 0) then
    echo "ERROR: non-Full files in Full"
    exit 1
endif

#
# Verify no non-Snapshot files in "Snapshot"
#
if (`find $packageDir/RF2Release/Snapshot -name "*txt" | grep -v Snapshot | wc -l` > 0) then
    echo "ERROR: non-Snapshot files in Snapshot"
    exit 1
endif

#
# Verify no non-Delta files in "Delta"
#
if (`find $packageDir/RF2Release/Delta -name "*txt" | grep -v Delta | wc -l` > 0) then
    echo "ERROR: non-Delta files in Delta"
    exit 1
endif

#
# Verify all filenames out of Documentation should have "Icd10" in the name
#
if (`find $packageDir/RF2Release -name "*txt" | grep -v Icd10 | wc -l` > 0) then
    echo "ERROR: filenames without Icd10"
    find $packageDir/RF2Release -name "*txt" | grep -v Icd10 | sed 's/^/      /'
    exit 1
endif

#
# Verify all filenames out of Documentation have "_INT_" in the filename
#
if (`find $packageDir/RF2Release -name "*txt" | grep -v _INT_  | wc -l` > 0) then
    echo "ERROR: filenames without _INT_"
    find $packageDir/RF2Release -name "*txt" | grep -v _INT_  | sed 's/^/      /'
    exit 1
endif

#
# Verify all filenames out of Documentation have $version in the filename
#
if (`find $packageDir/RF2Release -name "*txt" | grep -v $version  | wc -l` > 0) then
    echo "ERROR: filenames without $version in filename"
    find $packageDir/RF2Release -name "*txt" | grep -v $version | sed 's/^/      /'
    exit 1
endif

#
# Verify snapshot/delta have same set of files
#
find $packageDir/RF2Release/Snapshot -name "*txt" | sed 's/Snapshot//g' | sort -u -o x$$.txt
find $packageDir/RF2Release/Full -name "*txt" | sed 's/Full//g' | sort -u -o y$$.txt
if (`diff x$$.txt y$$.txt | wc -l` > 0) then
    echo "ERROR: snapshot/full do not have same set of files"
    diff x$$.txt y$$.txt | sed 's/^/      /'
    exit 1
endif
/bin/rm -f x$$.txt y$$.txt

#
# Verify every ID used in Snapshot is unique and exists in full
#
cat `find $packageDir/RF2Release/Snapshot -name "*txt"` | grep -v id | cut -f 1 | sort >! x$$.txt
if (`uniq -d x$$.txt | wc -l` > 0) then
    echo "ERROR: non-unique IDs in Snapshot"
    uniq -d x$$.txt | sed 's/^/      /'
endif
cat `find $packageDir/RF2Release/Full -name "*txt"` | grep -v id | cut -f 1 | sort -u >! y$$.txt
if (`comm -23 x$$.txt y$$.txt | wc -l` > 0) then
    echo "ERROR: ids in snapshot not in full"
    comm -23 x$$.txt y$$.txt | sed 's/^/      /'
    exit 1
endif
if (`comm -23 y$$.txt x$$.txt | wc -l` > 0) then
    echo "ERROR: ids in full not in snapshot"
    comm -23 y$$.txt x$$.txt | sed 's/^/      /'
    exit 1
endif
/bin/rm -f x$$.txt y$$.txt

#
# Delta file checks
#
# TODO: remove the 1 == 0 after baseline package
if (1 == 0 && -e $packageDir/RF2Release/Delta) then

    #
    # Verify all delta rows have $version as effectiveTime
    #
    foreach f (`find $packageDir/RF2Release/Delta -name "*txt"`)
        if (`$PATH_TO_PERL -ne 'split /\t/; print unless $_[1] eq "'$version'";' $f | grep -v '^id' | wc -l` > 0) then
            echo "ERROR: delta file has effective time not matching $version"
            echo "      $f"
            exit 1
        endif
end

    #
    # Verify snapshot/delta have same set of files
    #
    find $packageDir/RF2Release/Snapshot -name "*txt" | sed 's/Snapshot//g' | sort -u -o x$$.txt
    find $packageDir/RF2Release/Delta -name "*txt" | sed 's/Delta//g' | sort -u -o y$$.txt
    if (`diff x$$.txt y$$.txt | wc -l` > 0) then
        echo "ERROR: snapshot/delta do not have same set of files"
        diff x$$.txt y$$.txt | sed 's/^/      /'
        exit 1
    endif
    /bin/rm -f x$$.txt y$$.txt

    #
    # Verify every ID from snapshot with current version should be in delta.
    #
    foreach f (`find $packageDir/RF2Release/Snapshot -name "*txt"`)
        set f2 = `echo $f | sed 's/Snapshot/Delta/`'
        head -1 $f >! x$$.txt
        grep $version $f >> x$$.txt
        sort -u -o x$$.txt x$$.txt
        if (`sort $f2 | diff - x$$.txt | wc -l`) then
            echo "ERROR: current version entries from snapshot do not match delta"
            sort $f2 | diff - x$$.txt | sed 's/^/      /'
            exit 1
        endif
end

endif


#
# OTHER THINGS TO CHECK
# * effective time of retired rows in full from prior snapshot should be current version
#


#
# Zip the file
#
echo "    Package .zip ...`/bin/date`"
pushd $packageDir:h >>& /dev/null
zip -r $packageDir:t.zip $packageDir:t >>& /dev/null
if ($status != 0) then
    echo "ERROR: failed to build .zip file"
    exit 1
endif
touch -t ${version}0000 $packageDir:t.zip
if ($status != 0) then
    echo "ERROR: problem setting timestamp on .zip file"
    exit 1
endif
popd >>& /dev/null

echo "----------------------------------------------"
echo "Finished $0 ... `/bin/date`"
echo "----------------------------------------------"
