#!/bin/tcsh -f
#
# RF2 QA for ExtendedMap ref sets
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
set usage =  "Usage: qaCrossMaps.csh [-core <coreDir>] <mapFile> <map descriptor file> <module dependencyfile>"

#
# Parse arguments
#
set pcCategoryId = 447637006
set cdCategoryId = 447639009
set quick = 0
set extDir=
set coreDir=
set i=1
while ($i <= $#argv)
    switch ($argv[$i])
        case '-*help':
            cat <<EOF
 This script performs QA checks on US Extension RF1 files.

  $usage

 Parameters:
    <mapFile>              : the mapping file iisssccRefSet (required)
    <mapDescriptorFile>    : the map category file cRefSet (required)
    <moduleDependencyFile> : the map category file cRefSet (required)

 Options
    -core                  : Set the core files directory (where INT SNOMED CT is) (optional)
    -[-]help               : Online help

EOF
            exit 0
        case '-namespace':
            set i=`expr $i + 1`
            set namespaceId=$argv[$i]
            breaksw
        case '-ext':
            set i=`expr $i + 1`
            set extDir=$argv[$i]
            breaksw
        case '-core':
            set i=`expr $i + 1`
            set coreDir=$argv[$i]
            breaksw
        default :
            set arg_count=3
            set all_args=`expr $i + $arg_count - 1`
            if ($all_args != $#argv) then
                echo "Error: Incorrect number of arguments: $all_args, $#argv"
                echo "$usage"
                exit 1
            endif
            set mapFile=$argv[$i]
            @ i = $i + 1
            set mapDescriptorFile = $argv[$i]
            @ i = $i + 1
            set moduleDependencyFile = $argv[$i]
    endsw
    set i=`expr $i + 1`
end

if ($?mapFile == 0) then
    echo "$usage"
    exit 1
endif

echo "----------------------------------------------------------------------"
echo "Starting ... `/bin/date`"
echo "----------------------------------------------------------------------"
echo "MapFile:              $mapFile"
echo "MapDescriptorFile:    $mapDescriptorFile"
echo "ModuleDependencyFile: $moduleDependencyFile"
echo "Ext dir:              $extDir"
echo "Core dir:             $coreDir"
echo ""

#
# Verify UTF8
#
echo "    Verify all characters are valid UTF8 terminology characters ...`/bin/date`"
$PATH_TO_PERL -MEncode -ne 'BEGIN { binmode(STDIN, ":raw:encoding(UTF-8)"); } \
    Encode::from_to($_,"utf8","UTF-16LE"); Encode::from_to($_,"UTF-16LE","utf8"); print;' $mapFile >&! $mapFile.2
set ct = `diff $mapFile $mapFile.2 | wc -l`
if ($ct != 0) then
    echo "ERROR: invalid UTF8 chars in map file"
    diff $mapFile $mapFile.2 | sed 's/^/      /'
endif
/bin/rm -f $mapFile.2
$PATH_TO_PERL -MEncode -ne 'BEGIN { binmode(STDIN, ":raw:encoding(UTF-8)"); } \
    Encode::from_to($_,"utf8","UTF-16LE"); Encode::from_to($_,"UTF-16LE","utf8"); print;' $mapDescriptorFile >&! $mapDescriptorFile.2
set ct = `diff $mapDescriptorFile $mapDescriptorFile.2 | wc -l`
if ($ct != 0) then
    echo "ERROR: invalid UTF8 chars in map file"
    diff $mapDescriptorFile $mapDescriptorFile.2 | sed 's/^/      /'
endif
/bin/rm -f $mapDescriptorFile.2

$PATH_TO_PERL -MEncode -ne 'BEGIN { binmode(STDIN, ":raw:encoding(UTF-8)"); } \
    Encode::from_to($_,"utf8","UTF-16LE"); Encode::from_to($_,"UTF-16LE","utf8"); print;' $moduleDependencyFile >&! $moduleDependencyFile.2
set ct = `diff $moduleDependencyFile $moduleDependencyFile.2 | wc -l`
if ($ct != 0) then
    echo "ERROR: invalid UTF8 chars in map file"
    diff $moduleDependencyFile $moduleDependencyFile.2 | sed 's/^/      /'
endif
/bin/rm -f $moduleDependencyFile.2

#
# Verify line termination.
#
echo "    Verify line termination ...`/bin/date`"
set ct = `$PATH_TO_PERL -ne 'print unless /\r\n/;' $mapFile | wc -l`
if ($ct != 0) then
    echo "ERROR: invalid line termination in map file"
    $PATH_TO_PERL -ne 'print unless /\r\n/;' $mapFile | sed 's/^/      /'
endif

set ct = `$PATH_TO_PERL -ne 'print unless /\r\n/;' $mapDescriptorFile | wc -l`
if ($ct != 0) then
    echo "ERROR: invalid line termination in map file"
    $PATH_TO_PERL -ne 'print unless /\r\n/;' $mapDescriptorFile | sed 's/^/      /'
endif

set ct = `$PATH_TO_PERL -ne 'print unless /\r\n/;' $moduleDependencyFile | wc -l`
if ($ct != 0) then
    echo "ERROR: invalid line termination in map file"
    $PATH_TO_PERL -ne 'print unless /\r\n/;' $moduleDependencyFile | sed 's/^/      /'
endif

#
# Verify column headers
#
echo "    Verify column headers (all camelCase) ...`/bin/date`"
head -1 $mapFile | $PATH_TO_PERL -pe 's/\r\n/\n/;' >&! x.txt
echo "id        effectiveTime   active  moduleId        refSetId        referencedComponentId   mapGroup        mapPriority      mapRule mapAdvice       mapTarget       correlationId   mapCategoryId" >&! x2.txt
set ct = `diff x.txt x2.txt | wc -l`
if ($ct != 0) then
    echo "ERROR: invalid column headers"
    diff x.txt x2.txt | sed 's/^/      /'
endif
/bin/rm -f x.txt x2.txt

head -1 $mapDescriptorFile | $PATH_TO_PERL -pe 's/\r\n/\n/;' >&! x.txt
echo "id        effectiveTime   active  moduleId        refSetId        referencedComponentId   attributeDescription    attributeType    attributeOrder" >&! x2.txt
set ct = `diff x.txt x2.txt | wc -l`
if ($ct != 0) then
    echo "ERROR: invalid column headers"
    diff x.txt x2.txt | sed 's/^/      /'
endif
/bin/rm -f x.txt x2.txt

head -1 $moduleDependencyFile | $PATH_TO_PERL -pe 's/\r\n/\n/;' >&! x.txt
echo "id        effectiveTime   active  moduleId        refSetId        referencedComponentId   sourceEffectiveTime     targetEffectiveTime" >&! x2.txt
set ct = `diff x.txt x2.txt | wc -l`
if ($ct != 0) then
    echo "ERROR: invalid column headers"
    diff x.txt x2.txt | sed 's/^/      /'
endif
/bin/rm -f x.txt x2.txt


#
# Verify sort order
#
echo "    Verify sort order ...`/bin/date`"
grep -v -i referencedComponentId $mapFile | /bin/sort -c -t\     -k 5,5 -k 6,6n -k 7,7n -k 8,8n -k 1,4 -k 9,9 -k 10,10 -k 11,11 -k 12,12 -k 13,13
if ($status != 0) then
    echo "ERROR: sort order in $mapFile"
endif

grep -v -i referencedComponentId $mapDescriptorFile | /bin/sort -c -t\   -k 5,5 -k 6,6 -k 9,9n -k 1,4
if ($status != 0) then
    echo "ERROR: sort order in $mapDescriptorFile"
endif

grep -v -i referencedComponentId $moduleDependencyFile | /bin/sort -c -t\        -k 5,8 -k 1,4
if ($status != 0) then
    echo "ERROR: sort order in $moduleDependencyFile"
endif


#
# Verify UUID
#
echo "    Verify UUID ...`/bin/date`"
$PATH_TO_PERL -ne 'BEGIN { use lib "$ENV{MAPPING_HOME}/lib"; use UUID::Tiny; binmode(STDIN, ":raw:encoding(UTF-8)"); } \
  split /\t/; $uuid = UUID::Tiny::UUID_to_string(UUID::Tiny::create_UUID(5, "$_[4]$_[5]$_[6]$_[8]$_[10]")); \
  print "$_[4]$_[5]$_[6]$_[8]$_[10] : $uuid (not $_[0])\n" if $uuid ne $_[0];' $mapFile | grep -v -i referencedComponentId >&! x.txt
set ct = `cat x.txt | wc -l`
if ($ct > 0) then
    echo "ERROR: UUID computed incorrectly"
    cat x.txt | sed 's/^/      /'
endif
/bin/rm -f x.txt
set ct = `$PATH_TO_PERL -ne 'split /\t/; print "$_[0]\n";' $mapFile | sort | uniq -d | wc -l`
if ($ct > 0) then
    echo "ERROR: UUID not unique"
    $PATH_TO_PERL -ne 'split /\t/; print "$_[0]\n";' $mapFile | sort | uniq -d | sed 's/^/      /'
endif
/bin/rm -f x.txt

$PATH_TO_PERL -ne 'BEGIN { use lib "$ENV{MAPPING_HOME}/lib"; use UUID::Tiny; binmode(STDIN, ":raw:encoding(UTF-8)"); } \
    s/[\r\n]//g;  split /\t/;  $uuid = UUID::Tiny::UUID_to_string(UUID::Tiny::create_UUID(5, "$_[3]$_[4]$_[5]")); \
    print "$_[3]$_[4]$_[5] : $uuid (not $_[0])\n" if $uuid ne $_[0];' $moduleDependencyFile | grep -v -i referencedComponentId >&! x.txt
set ct = `cat x.txt | wc -l`
if ($ct > 0) then
    echo "ERROR: UUID computed incorrectly"
    cat x.txt | sed 's/^/      /'
endif
/bin/rm -f x.txt
set ct = `$PATH_TO_PERL -ne 'split /\t/; print "$_[0]\n";' $moduleDependencyFile | sort | uniq -d | wc -l`
if ($ct > 0) then
    echo "ERROR: UUID not unique"
    $PATH_TO_PERL -ne 'split /\t/; print "$_[0]\n";' $moduleDependencyFile | sort | uniq -d | sed 's/^/      /'
endif
/bin/rm -f x.txt

$PATH_TO_PERL -ne 'BEGIN { use lib "$ENV{MAPPING_HOME}/lib"; use UUID::Tiny; binmode(STDIN, ":raw:encoding(UTF-8)"); } \
    s/[\r\n]//g;  split /\t/;  $uuid = UUID::Tiny::UUID_to_string(UUID::Tiny::create_UUID(5, "$_[4]$_[5]$_[6]$_[7]$_[8]")); \
    print "$_[4]$_[5]$_[6]$_[7]$_[8] : $uuid (not $_[0])\n" if $uuid ne $_[0];' $mapDescriptorFile | grep -v -i referencedComponentId >&! x.txt
set ct = `cat x.txt | wc -l`
if ($ct > 0) then
    echo "ERROR: UUID computed incorrectly"
    cat x.txt | sed 's/^/      /'
endif
/bin/rm -f x.txt
set ct = `$PATH_TO_PERL -ne 'split /\t/; print "$_[0]\n";' $mapDescriptorFile | sort | uniq -d | wc -l`
if ($ct > 0) then
    echo "ERROR: UUID not unique"
    $PATH_TO_PERL -ne 'split /\t/; print "$_[0]\n";' $mapDescriptorFile | sort | uniq -d | sed 's/^/      /'
endif
/bin/rm -f x.txt

#
# Verify all entries are active
#
echo "    Verify all entries are active ...`/bin/date`"
set ct = `$PATH_TO_PERL -ne 'split /\t/; print unless $_[2] == 1;' $mapFile | grep -v -i referencedComponentId | wc -l`
if ($ct != 0) then
    echo "ERROR: entries without active=1"
    $PATH_TO_PERL -ne 'split /\t/; print unless $_[2] == 1;' $mapFile | sed 's/^/      /'
endif

set ct = `$PATH_TO_PERL -ne 'split /\t/; print unless $_[2] == 1;' $mapDescriptorFile | grep -v -i referencedComponentId | wc -l`
if ($ct != 0) then
    echo "ERROR: entries without active=1"
    $PATH_TO_PERL -ne 'split /\t/; print unless $_[2] == 1;' $mapDescriptorFile | sed 's/^/      /'
endif

set ct = `$PATH_TO_PERL -ne 'split /\t/; print unless $_[2] == 1;' $moduleDependencyFile | grep -v -i referencedComponentId | wc -l`
if ($ct != 0) then
    echo "ERROR: entries without active=1"
    $PATH_TO_PERL -ne 'split /\t/; print unless $_[2] == 1;' $moduleDependencyFile | sed 's/^/      /'
endif

#
# Verify max effectiveTime matches the file date
#
echo "    Verify max effectiveTime matches the file date ...`/bin/date`"
foreach file ($mapFile $mapDescriptorFile $moduleDependencyFile)
    set maxEffectiveTime = `$PATH_TO_PERL -ne 'split /\t/; print "$_[1]\n"' $file | grep -v -i effectiveTime | sort -u | tail -1`
    set fileDate = `echo $file | $PATH_TO_PERL -ne 's/.*_(\d+)\.txt/$1/; print;'`
    if ("x$maxEffectiveTime" == "x") then
        echo "ERROR: maxEffectiveTime is blank"
    endif
    if ("x$fileDate" == "x") then
        echo "ERROR: fileDate is blank"
    endif
    if ($maxEffectiveTime != $fileDate) then
        echo "ERROR: max effectiveTime does not match file date"
        echo "       $maxEffectiveTime  != $file"
    endif
end

#
# Verify moduleId is valid
# (is an active child of the "module ids" concept)
#
echo "    Verify moduleId is valid ...`/bin/date`"
if ($coreDir != "") then
    # if RF2
    if (`head -1 $coreDir/*Concept*txt | $PATH_TO_PERL -ne 'split /\t/; print scalar(@_),"\n";'` == 5) then
        $PATH_TO_PERL -ne 'split /\t/; print "$_[3]\n";' $mapFile | grep -v -i moduleId | sort -u -o x.txt
        $PATH_TO_PERL -ne 'split /\t/; print "$_[0]\n";' $coreDir/*Concept*txt | grep -v -i CONCEPTID | sort -u -o x2.txt
        set ihtsdoModuleParent = 900000000000445007
        set isaRel = 116680003
        $PATH_TO_PERL -ne 'split /\t/; print "$_[4]\n" if $_[5] eq "'$ihtsdoModuleParent'" && $_[7] eq "'$isaRel'";' $coreDir/*Relationship*txt | grep -v -i CONCEPTID | sort -u -o x3.txt
        set ct = `comm -23 x.txt x2.txt | wc -l`
        if ($ct > 0) then
            echo "ERROR: moduleId not in core concepts file"
            comm -23 x.txt x2.txt | sed 's/^/      /'
        endif
        set ct = `comm -23 x.txt x3.txt | wc -l`
        if ($ct > 0) then
            echo "ERROR: moduleId not child of $ihtsdoModuleParent"
            comm -23 x.txt x3.txt | sed 's/^/      /'
        endif
        /bin/rm -f x.txt x2.txt x3.txt
    else
        echo "      SKIP TEST: core directory has RF1 data"
    endif
else
    echo "      SKIP TEST: no -core flag specified"
endif

#
# Verify refSetId is valid
# (is an active child of the "complex map type ref set" concept)
#
echo "    Verify refSetId is valid ...`/bin/date`"
if ($coreDir != "") then
    # if RF2
    if (`head -1 $coreDir/*Concept*txt | $PATH_TO_PERL -ne 'split /\t/; print scalar(@_),"\n";'` == 5) then
        $PATH_TO_PERL -ne 'split /\t/; print "$_[4]\n";' $mapFile | grep -v -i refSetId | sort -u -o x.txt
        $PATH_TO_PERL -ne 'split /\t/; print "$_[0]\n";' $coreDir/*Concept*txt | grep -v -i CONCEPTID | sort -u -o x2.txt
        set ihtsdoRefSetParent = 447250001
        set isaRel = 116680003
        $PATH_TO_PERL -ne 'split /\t/; print "$_[4]\n" if $_[5] eq "'$ihtsdoRefSetParent'" && $_[7] eq "'$isaRel'";' $coreDir/*Relationship*txt | grep -v -i CONCEPTID | sort -u -o x3.txt
        set ct = `comm -23 x.txt x2.txt | wc -l`
        if ($ct > 0) then
            echo "ERROR: refSetId not in core concepts file"
            comm -23 x.txt x2.txt | sed 's/^/      /'
        endif
        set ct = `comm -23 x.txt x3.txt | wc -l`
        if ($ct > 0) then
            echo "ERROR: refSetId not child of $ihtsdoRefSetParent"
            comm -23 x.txt x3.txt | sed 's/^/      /'
        endif

        /bin/rm -f x.txt x2.txt x3.txt
    else
        echo "      SKIP TEST: core directory has RF1 data"
    endif
else
    echo "      SKIP TEST: no -core flag specified"
endif


#
# Verify referencedComponentId is in RF2 $coreDir/concepts file
#
echo "    Verify referencedComponentId in RF2 concepts file ...`/bin/date`"
if ($coreDir != "") then
    $PATH_TO_PERL -ne 'split /\t/; print "$_[5]\n";' $mapFile | grep -v -i REFERENCED | sort -u -o x.txt
    $PATH_TO_PERL -ne 'split /\t/; print "$_[0]\n";' $coreDir/*Concept*txt | grep -v -i CONCEPTID | sort -u -o x2.txt
    set ct = `comm -23 x.txt x2.txt | wc -l`
    if ($ct > 0) then
        echo "ERROR: referencedComponentId not in core concepts file"
        comm -23 x.txt x2.txt | sed 's/^/      /'
    endif
    /bin/rm -f x.txt x2.txt
else
    echo "      SKIP TEST: no -core flag specified"
endif

#
# Verify mapTarget is not null when mapCategory is $pcCategoryId or $cdCategoryId
#
echo "    Verify mapTarget is not null when mapCategory is $pcCategoryId or $cdCategoryId"
$PATH_TO_PERL -ne 's/[\r\n]//g; split /\t/; print "$_[0]\n" if ($_[12] eq "'$pcCategoryId'" or $_[12] eq "'$cdCategoryId'") \
   && ! $_[10];' $mapFile | sort -u -o x.txt
set ct = `cat x.txt | wc -l`
if ($ct > 0) then
    echo "      ERROR: Map entries with null mapTarget and PC or CD mapCategory"
    cat x.txt | sed 's/^/      /'
endif

#
# Verify mapTarget is null when mapCategory is not $pcCategoryId or $cdCategoryId
#
echo "    Verify mapTarget is null when mapCategory is not $pcCategoryId or $cdCategoryId"
$PATH_TO_PERL -ne 's/[\r\n]//g; split /\t/; print "$_[0]\n" if $_[12] ne "'$pcCategoryId'" && $_[12] ne "'$cdCategoryId'" \
   && $_[10] && $_[0] ne "id";' $mapFile | sort -u -o x.txt
set ct = `cat x.txt | wc -l`
if ($ct > 0) then
    echo "      ERROR: Map entries with non-null target without $pcCategoryId or $cdCategoryId mapCategory"
    cat x.txt | sed 's/^/      /'
endif

#
# Verify mapCategory is in supported list
#    Outside of the scope of ICD-10 (447636002 |Map concept is outside scope of target classification|)
#    The map source concept is properly classified (447637006 |Map source concept is properly classified|)
#    The map source cannot be classified (447638001 |Map source concept cannot be classified with available data|
#    The map is context dependent (447639009 |Map of source concept is context dependent|)
#    The source concept is ambiguous (447640006 |Source SNOMED concept is ambiguous|)
#    The source SNOMED CT concept is incompletely modeled (447641005 |Source SNOMED concept is incompletely modeled|)
#    Guidance from WHO is ambiguous (447635003 |Mapping guidance from WHO is ambiguous|)
#    Retired from map scope (447642003 |Source concept has been retired from map scope|);
#
echo "    Verify that mapCategory is in supported list ...`/bin/date`"
$PATH_TO_PERL -ne 'split /\t/; $_[12] =~ s/[\r\n]//g; print "$_[12]\n";' $mapFile |\
    grep -v -i valueId | grep -v 447636002 | grep -v 447637006 | grep -v 447638001 |\
    grep -v 447639009 | grep -v 447640006 | grep -v 447641005 | grep -v 447635003 |\
    grep -v 447642003 | grep -v mapCategory >! x.txt
set ct = `cat x.txt | wc -l`
if ($ct != 0) then
    echo "ERROR: bad mapCategory value"
    cat x.txt | sed 's/^/      /'
endif

#
# Verify IFA rules have $cdCategoryId
#
echo "    Verify IFA rules with mapTargets have $cdCategoryId mapCategory ...`/bin/date`"
$PATH_TO_PERL -ne 's/[\r\n]//g; split /\t/; print "$_[0]\n" if $_[8] =~ /^IFA / && $_[10] && $_[12] ne "'$cdCategoryId'";' $mapFile | sort -u -o x.txt
set ct = `cat x.txt| wc -l`
if ($ct > 0) then
    echo "      ERROR: Map entries with null mapTarget and invalid mapCategory"
    cat x.txt x2.txt | sed 's/^/      /'
endif

#
# Verify concepts in IFA rules also have top level entries
#
echo "    Verify concepts in IFA rules also have top level entries ...`/bin/date`"
cut -f 9 $mapFile | grep IFA | perl -pe 's/IFA (\d*) \|.*/$1/' |\
  grep -v 248152002 |\
  grep -v 248153007 |\
  grep -v 445518008 | sort -u -o x.$$.txt
cut -f 6 $mapFile | sort -u -o x2.$$.txt
set ct = `comm -23 x.$$.txt x2.$$.txt | wc -l`
if ($ct > 0) then
    echo "      ERROR: concepts in IFA rules that do not have top level records"
    comm -23 x.$$.txt x2.$$.txt | sed 's/^/      /'
endif
/bin/rm -f x.$$.txt x2.$$.txt

#
# Verify no descendants in map records are retired
#
echo "    Verify no descendants in map records are retired ...`/bin/date`"
$PATH_TO_PERL -ne 'split /\t/; print "$_[0]\n" if $_[2] eq "0";' $coreDir/*Concept*txt | sort -u -o x.$$.txt
set ct = `cut -f 6 $mapFile | sort -u | comm -12 - x.$$.txt | wc -l`
if ($ct != 0) then
    echo "ERROR: Map records with concepts that are retired"
    cut -f 6 $mapFile | sort -u | comm -12 - x.$$.txt | sed 's/^/      /'
endif
/bin/rm -f x.$$.txt

#
# Verify higher map groups do not have only NC nodes
#
echo "    Verify higher map groups do not have only NC nodes ...`/bin/date`"
$PATH_TO_PERL -ne 'split /\t/; print "$_[5]|$_[6]\n" if $_[6] > 1 && $_[9] =~ /NOT BE CLASSIFIED/' $mapFile |\
  sort -u -o x.$$.txt
$PATH_TO_PERL -ne 'split /\t/; print "$_[5]|$_[6]\n" if $_[6] > 1 && $_[9] !~ /NOT BE CLASSIFIED/' $mapFile |\
  sort -u -o x2.$$.txt
set ct = `comm -23 x.$$.txt x2.$$.txt | wc -l`
if ($ct != 0) then
    echo "ERROR: Concepts with higher groups that have only NC entries"
    comm -23 x.$$.txt x2.$$.txt | sed 's/^/      /'
endif
/bin/rm -rf x.$$.txt x2.$$.txt

#
# For RULE based mappings
#
set ct = `$PATH_TO_PERL -ne 'split /\t/; if ($_[8] ne "" && $_[8] !~ /maprule/i) { print "1\n"; } else { print "0\n";} ' $mapFile | head | grep -c 1`
if ($ct > 0) then

#
# Each concept has at least one mapGroup
#
echo "    Verify each concept has at least one mapGroup ...`/bin/date`"
set ct1=`$PATH_TO_PERL -ne 'split /\t/; print "$_[5]|$_[6]\n" if $_[6] =~ /\d{1,}/' $mapFile | cut -d\| -f 1 | sort -u | wc -l`
set ct2=`$PATH_TO_PERL -ne 'split /\t/; print "$_[5]\n"' $mapFile | grep -v -i referencedComponentId | sort -u | wc -l`
if ($ct1 != $ct2) then
    echo "ERROR: Not all map groups have at least one mapping
endif

#
# All mapGroups are numbered as consecutive integers starting from 1
# So mapGroup either equasl 1, or the previous value, or the previous value + 1
#
echo "    Verify mapGroups are numbered as consecutive integers starting from 1 ...`/bin/date`"
set ct=`$PATH_TO_PERL -ne 'split /\t/; print unless $_[6] == 1 || $_[6] == ($prevGroup+1) || $_[6] == $prevGroup; $prevGroup = $_[6];' $mapFile | grep -v -i refSetId | wc -l`
if ($ct != 0) then
    echo "ERROR: mapGroup is not numbered as consecutive integers starting with 1"
    $PATH_TO_PERL -ne 'split /\t/; print unless $_[6] == 1 || $_[6] == ($prevGroup+1) || $_[6] == $prevGroup; $prevGroup = $_[6];' $mapFile |\
       sed 's/^/      /'
endif

#
# Each mapGroup has at least one mapPriority (with a rule)
#
echo "    Verify each mapGroup has at least one mapPriority (with a rule) ...`/bin/date`"
set ct1=`$PATH_TO_PERL -ne 'split /\t/; print "$_[5]|$_[6]|$_[7]\n" if $_[7] =~ /\d{1,}/' $mapFile | cut -d\| -f 1,2 | sort -u | wc -l`
set ct2=`$PATH_TO_PERL -ne 'split /\t/; print "$_[5]|$_[6]\n"' $mapFile | grep -v -i referencedComponentId | sort -u | wc -l`
if ($ct1 != $ct2) then
    echo "ERROR: Not all map groups have at least one rule"
endif

#
# All mapPriorities are numbered as consecutive integers starting from 1
#
echo "    Verify all mapPriorities are numbered as consecutive integers starting from 1 ... `/bin/date`"
set ct=`$PATH_TO_PERL -ne 'split /\t/; print unless $_[7] == 1 || $_[7] == ($prevGroup+1); $prevGroup = $_[7];' $mapFile | grep -v -i refSetId | wc -l`
if ($ct != 0) then
    echo "ERROR: mapPriority is not numbered as consecutive integers starting with 1"
    $PATH_TO_PERL -ne 'split /\t/; print unless $_[7] == 1 || $_[7] == ($prevGroup+1); $prevGroup = $_[7];' $mapFile |\
        grep -v -i refSetId | sed 's/^/      /'
endif

#
# TRUE or OTHERWISE TRUE rules do not appear before IFA rules
#
echo "    Verify TRUE rules do not appear before IFA rules ... `/bin/date`"
set ct=`$PATH_TO_PERL -ne 'split /\t/; print $prevLine if $_[8] =~ /IFA/ && $prevCidGRule =~ /$_[5]$_[6].*TRUE/; $prevLine = $_; $prevCidGRule = "$_[5]$_[6]$_[8]";' $mapFile | grep -v -i refSetId | wc -l`
if ($ct != 0) then
    echo "ERROR: TRUE rule appears before end of group"
    $PATH_TO_PERL -ne 'split /\t/; print $prevLine if $_[8] =~ /IFA/ && $prevCidGRule =~ /$_[5]$_[6].*TRUE/; $prevLine = $_; $prevCidGRule = "$_[5]$_[6]$_[8]";' $mapFile |\
      grep -v -i refSetId | sed 's/^/      /'
endif

#
# The last entry in a mapGroup is either TRUE or OTHERWISE TRUE
#
echo "    Verify the last entry in a mapGroup is either TRUE or OTHERWISE TRUE ...`/bin/date`"
set ct=`$PATH_TO_PERL -ne 'split /\t/; print $prevLine if $prevRule ne "TRUE" && $prevRule ne "OTHERWISE TRUE" && "$_[5]$_[6]" ne $prevCGroup; $prevCGroup = "$_[5]$_[6]"; $prevRule = $_[8]; $prevLine = $_' $mapFile | grep -v -i refSetId | wc -l`
if ($ct != 0) then
    echo "ERROR: group does not end in TRUE rule"
    $PATH_TO_PERL -ne 'split /\t/; print $prevLine if $prevRule ne "TRUE" && $prevRule ne "OTHERWISE TRUE" && "$_[5]$_[6]" ne $prevCGroup; $prevCGroup = "$_[5]$_[6]"; $prevRule = $_[8]; $prevLine = $_' $mapFile | grep -v -i refSetId | sed 's/^/      /'
endif

#
# Verify IFA rules using descendant-or-self are not leaf nodes
# RULE DISABLED - "OR DESCENDANT" no longer used
#echo "    Verify IFA rules using descendant-or-self are not leaf nodes ...`/bin/date`"
#if ($coreDir != "") then
#    grep 'OR DESCENDANT' $mapFile | sed 's/.*IFA //; s/ \|.*//;' | sort -u >! x.txt
#    $MAPPING_HOME/bin/descHelper.pl x.txt $coreDir/*_Relation*txt CT | grep '|0' | cut -d\| -f 1 | sort -o y.txt
#    set ct = `cat y.txt | wc -l`
#    if ($ct > 0) then
#       echo "ERROR: IFA rules using descendant-or-self that are also leaf nodes"
#       cat y.txt | sed 's/^/      /'
#    endif
#    /bin/rm -f [xy].txt
#else
#    echo "      SKIP TEST: no -core flag specified"
#endif

#
# Verify IFA rules refer to valid SctIds from
#
echo "    Verify IFA rules refer to valid conceptId ...`/bin/date`"
if ($coreDir != "") then

    grep 'IFA [0-9]' $mapFile | $PATH_TO_PERL -pe 's/.*IFA //; s/ \|.*//;' | sort -u -o x.txt
    $PATH_TO_PERL -ne 'split /\t/; print "$_[0]\n";' $coreDir/*Concept*txt | sort -u -o y.txt
    set ct = `comm -23 x.txt y.txt | wc -l`
    if ($ct > 0) then
        echo "ERROR: IFA rules contain invalid concept ids"
        comm -23 x.txt y.txt | sed 's/^/      /'
    endif
    /bin/rm -f [xy].txt
else
    echo "      SKIP TEST: no -core flag specified"
endif

#
# AGE rules do not end with <= 0
#
echo "    Verify AGE rules do not end with <= 0 ...`/bin/date`"
set ct = `grep 'age at onset of clinical finding' $mapFile | $PATH_TO_PERL -ne 'split /\t/; print if $_[8] =~ /<= 0 [a-z]*$/' | wc -l`
if ($ct != 0 ) then
    echo "ERROR: AGE rule ending in <= 0"
    grep 'age at onset of clinical finding' $mapFile | $PATH_TO_PERL -ne 'split /\t/; print if $_[8] =~ /<= 0 [a-z]*$/' | sed 's/^/      /'
endif


#
# Each mapRule has valid syntax
#
echo "    Verify each mapRule has valid syntax ...`/bin/date`"
$PATH_TO_PERL -ne 'BEGIN { use lib "$ENV{MAPPING_HOME}/lib"; use IHTSDO::MapRule; } \
    split /\t/; print unless IHTSDO::MapRule::isValidRule($_[8]); ' $mapFile | grep -v -i referencedComponentId   >&! x.txt
set ct = `cat x.txt | wc -l`
if ($ct != 0 ) then
    echo "ERROR: invalid rule format"
    cat x.txt | grep -v -i referencedComponentId | sed 's/^/      /'
endif
/bin/rm -f x.txt

#
# mapAdvice is restricted to the defined list
#
echo "    Verify mapAdvice is restricted to the defined list ...`/bin/date`"
echo "    Verify multiple map advice is properly handled ...`/bin/date`"
$PATH_TO_PERL -ne 'split /\t/; @a = split /\|/, $_[9]; $i = 0; \
     foreach $a (@a) { $a =~ s/ $//g; print "$a\n" unless (($a =~ /^IF/ || $a =~ /^ALWAYS/) && $i++ == 0); }' $mapFile |\
     grep -i -v mapAdvice | grep -v '^ 46' | \
  fgrep -v -f $MAPPING_HOME/etc/mapAdvice.txt >&! x.txt
set ct = `cat x.txt | wc -l`
if ($ct > 0) then
    echo "ERROR: Invalid map advice"
    cat x.txt | sed 's/^/      /'
endif
/bin/rm -f x.txt

#
# Map advice is not duplicated
#
echo "    Verify mapAdvice is not duplicated ...`/bin/date`"
$PATH_TO_PERL -ne 'split /\t/; @a = split /\|/, $_[9]; $i = 0; \
     foreach $a (@a) { $a =~ s/^ +//; print "$_[0]|$a\n" unless (($a =~ /^IF/ || $a =~ /^ALWAYS/) && $i++ == 0); }' $mapFile |\
     sort | uniq -d >! x.txt
set ct = `cat x.txt | wc -l`
if ($ct > 0) then
    echo "ERROR: Duplicated map advice"
    cat x.txt | sed 's/^/      /'
endif
/bin/rm -f x.txt

#
# Verify NC map advice
#
echo "    Verify NC has valid map advice ...`/bin/date`"
grep 'NOT BE CLASSIFIED' $mapFile |\
   grep '|' | $PATH_TO_PERL -ne 'split /\t/; print "$_[0]|$_[9]\n"' |\
   $PATH_TO_PERL -pe 's/MAP IS CONTEXT DEPENDENT FOR GENDER \| //' |\
   $PATH_TO_PERL -pe 's/MAP SOURCE CONCEPT CANNOT BE CLASSIFIED WITH AVAILABLE DATA//' |\
   $PATH_TO_PERL -pe 's/ \| //g' | grep -v '|$' >! x.txt
set ct = `cat x.txt | wc -l`
if ($ct > 0) then
    echo "ERROR: Invalid map advice in conjunction with NC case"
    cat x.txt | sed 's/^/      /'
endif
/bin/rm -f x.txt

#
# Verify AWH map advice
#
echo "    Verify AWH has valid map advice ...`/bin/date`"
grep 'WHO IS AMBIGUOUS' $mapFile |\
   grep '|' | $PATH_TO_PERL -ne 'split /\t/; print "$_[0]|$_[9]\n"' |\
   $PATH_TO_PERL -pe 's/MAP IS CONTEXT DEPENDENT FOR GENDER \| //' |\
   $PATH_TO_PERL -pe 's/MAPPING GUIDANCE FROM WHO IS AMBIGUOUS//' |\
   $PATH_TO_PERL -pe 's/ \| //g' | grep -v '|$' >! x.txt
set ct = `cat x.txt | wc -l`
if ($ct > 0) then
    echo "ERROR: Invalid map advice in conjunction with AWH case"
    cat x.txt | sed 's/^/      /'
endif
/bin/rm -f x.txt

#
# Verify ACT map advice
#
echo "    Verify ACT has valid map advice ...`/bin/date`"
grep 'SNOMED CONCEPT IS AMBIGUOUS' $mapFile |\
   grep '|' | $PATH_TO_PERL -ne 'split /\t/; print "$_[0]|$_[9]\n"' |\
   $PATH_TO_PERL -pe 's/MAP IS CONTEXT DEPENDENT FOR GENDER \| //' |\
   $PATH_TO_PERL -pe 's/SOURCE SNOMED CONCEPT IS AMBIGUOUS//' |\
   $PATH_TO_PERL -pe 's/ \| //g' | grep -v '|$' >! x.txt
set ct = `cat x.txt | wc -l`
if ($ct > 0) then
    echo "ERROR: Invalid map advice in conjunction with AWH case"
    cat x.txt | sed 's/^/      /'
endif
/bin/rm -f x.txt

#
# OS map category is not used
# Identifiy this via map advice: MAP CONCEPT IS OUTSIDE SCOPE OF TARGET CLASSIFICATION
#
echo "    Verify OS map category is not used ...`/bin/date`"
set ct = `grep 'MAP CONCEPT IS OUTSIDE SCOPE OF TARGET CLASSIFICATION' $mapFile | wc -l`
if ($ct > 0) then
    echo "ERROR: OS map category is still used"
    grep 'MAP CONCEPT IS OUTSIDE SCOPE OF TARGET CLASSIFICATION' $mapFile | sed 's/^/      /'
endif


#
# HLC concepts must not have explicit concept exclusion rules.
#
echo "    Verify HLC concepts must not have explicit concept exclusion rules ...`/bin/date`"
$PATH_TO_PERL -ne 'split /\t/; print "$_[5]\n" if $_[8] =~ /^IFA/ && \
    $_[8] !~ /Age at onset of clinical finding/ && \
    $_[8] !~ /Male \(finding\)/ && $_[8] !~ /Female \(finding\)/' $mapFile|\
  sort -u -o x.$$.txt
# exceptions are things that became HLC after latest SNOMED update, keep legitimate exceptions here
grep 'hlcConceptWithExplicitRules' $MAPPING_HOME/etc/history/qaCrossMapsExceptions.txt | cut -d\| -f 2 |\
  sort -u -o exceptions.$$.txt
echo "      exception count="`comm -12 x.$$.txt exceptions.$$.txt | wc -l`
comm -23 x.$$.txt exceptions.$$.txt >! y.$$.txt
/bin/mv -f y.$$.txt x.$$.txt
set ct = `comm -12 $MAPPING_HOME/etc/hlc.txt x.$$.txt | wc -l`
if ($ct != 0) then
    echo "ERROR: HLC concept with explicit concept exclusion rules"
    comm -12 $MAPPING_HOME/etc/hlc.txt x.$$.txt | sed 's/^/      /'
endif
/bin/rm -f x2.txt x.$$.txt exceptions.$$.txt

#
# NC default rule in group 1 must not be accompanied by non-NC default rule in later group.
# When this gets reported, it may be an LLC descendant, find the corresponding ancestor record for fixing it
# NOTE: cases that involve age or gender rules should be excluded
#
echo "    Verify NC default rule in group 1 must not be accompanied by non-NC default rule in later group ...`/bin/date`"
$PATH_TO_PERL -ne 'split /\t/; print "$_[5]\n" if $_[8] =~ /^(OTHERWISE TRUE|TRUE)$/ && \
    $_[9] =~ /MAP SOURCE CONCEPT CANNOT BE CLASSIFIED WITH AVAILABLE DATA/ && $_[6] == 1' $mapFile |\
    sort -u -o x.$$.txt
$PATH_TO_PERL -ne 'split /\t/; print "$_[5]\n" if $_[8] =~ /^(OTHERWISE TRUE|TRUE)$/ && \
    $_[9] !~ /MAP SOURCE CONCEPT CANNOT BE CLASSIFIED WITH AVAILABLE DATA/ && $_[6] >= 1' $mapFile |\
    sort -u -o x2.$$.txt
$PATH_TO_PERL -ne 'split /\t/; print "$_[5]\n" if $_[8] =~ /(Age at onset of clinical finding|248153007|248152002)/ && $_[6] == 1' $mapFile |\
    sort -u -o x3.$$.txt
set ct = `comm -23 x.$$.txt x3.$$.txt | comm -12 - x2.$$.txt | wc -l`
if ($ct != 0) then
    echo "ERROR: cases of NC default rule in group 1 with non-NC default rule in later group"
    comm -23 x.$$.txt x3.$$.txt | comm -12 - x2.$$.txt | sed 's/^/      /'
endif
/bin/rm -f x.$$.txt x2.$$.txt x3.$$.txt

#
# Map records must not have identical map targets for the same thing in both group 1 and a later group.
# When this gets reported, it may be an LLC descendant, find the corresponding ancestor record for fixing it
#
echo "    Verify Map records must not have identical map targets for the same thing in both group 1 and a later group ...`/bin/date`"
$PATH_TO_PERL -ne 'split /\t/; print "$_[5]|$_[8]|$_[10]\n" if $_[10] && $_[6] == 1' $mapFile | sed 's/OTHERWISE TRUE/TRUE/' | sort -u -o x.$$.txt
$PATH_TO_PERL -ne 'split /\t/; print "$_[5]|$_[8]|$_[10]\n" if $_[10] && $_[6] > 1' $mapFile | sed 's/OTHERWISE TRUE/TRUE/' | sort -u -o x2.$$.txt
set ct = `comm -12 x.$$.txt x2.$$.txt | wc -l`
if ($ct != 0) then
    echo "ERROR: cases of map records with identical map targets for the same thing in both group 1 and a later group"
    comm -12 x.$$.txt x2.$$.txt | sed 's/^/      /'
endif
/bin/rm -f x.$$.txt x2.$$.txt

#
# Map rule concept expressions must have ids matching names in the core descriptions file
#
echo "    Verify map rule concept expressions must have ids matching names in the core descriptions file ...`/bin/date`"
$PATH_TO_PERL -ne '\
  BEGIN { $infile = shift @ARGV; open ($IN, "$infile") || die "Could not open: $infile: $! $?\n"; \
          while (<$IN>) { split /\t/; $map{"$_[4]$_[7]"} = 1; } \
          close ($IN); } \
  split /\t/; $expr = $_[8]; while ($expr =~ s/IFA (\d+) \| ([^\|]+) \|//) { print "$_[0]|$_[8]\n" unless $map{"$1$2"}; } ' \
 $coreDir/*Desc*txt $mapFile | sort -u -o x.$$.txt
set ct = `cat x.$$.txt | wc -l`
if ($ct != 0) then
    echo "ERROR: mapRule not matching core descriptions file"
    cat x.$$.txt | sed 's/^/      /'
endif
/bin/rm -f x.$$.txt

#
# Advice MAP IS CONTEXT DEPENDENT FOR GENDER should only apply to IFA 248152002 | Female (finding) |, IFA 248153007 | Male (finding) | rules.
#
echo "    Verify advice MAP IS CONTEXT DEPENDENT FOR GENDER should only apply to gender rules ...`/bin/date`"
set ct = `grep 'MAP IS CONTEXT DEPENDENT FOR GENDER' $mapFile | grep -v 'Male (finding)' | grep -v 'Female (finding)' | wc -l`
if ($ct != 0) then
    echo "ERROR: gender advice is used for non gender rule"
    grep 'MAP IS CONTEXT DEPENDENT FOR GENDER' $mapFile | grep -v 'Male (finding)' | grep -v 'Female (finding)' | cut -f 1 | sed 's/^/      /'
endif
set ct = `grep 'Male (finding)' $mapFile | grep -v 'MAP IS CONTEXT DEPENDENT FOR GENDER' | wc -l`
if ($ct != 0) then
    echo "ERROR: male gender rule without gender advice"
    grep 'Male (finding)' $mapFile | grep -v 'MAP IS CONTEXT DEPENDENT FOR GENDER' | cut -f 1 | sed 's/^/      /'
endif
set ct = `grep 'Female (finding)' $mapFile | grep -v 'MAP IS CONTEXT DEPENDENT FOR GENDER' | wc -l`
if ($ct != 0) then
    echo "ERROR: female gender rule without gender advice"
    grep 'Female (finding)' $mapFile | grep -v 'MAP IS CONTEXT DEPENDENT FOR GENDER' | cut -f 1 | sed 's/^/      /'
endif

#
# Advice MAP IS CONTEXT DEPENDENT FOR GENDER is not used in conjunction with CD advice
#
echo "    Verify advice MAP IS CONTEXT DEPENDENT FOR GENDER is not used in conjunction with CD advice ...`/bin/date`"
set ct = `grep 'MAP IS CONTEXT DEPENDENT FOR GENDER' $mapFile | grep 'MAP OF SOURCE CONCEPT IS CONTEXT DEPENDENT' | wc -l`
if ($ct != 0) then
    echo "ERROR: Gender advice and CD advice both present"
    grep 'MAP IS CONTEXT DEPENDENT FOR GENDER' $mapFile | grep 'MAP OF SOURCE CONCEPT IS CONTEXT DEPENDENT' | cut -f 1 | sed 's/^/      /'
endif

#
# Map advice is sorted
#
echo "    Verify map advice is sorted ...`/bin/date`"
$PATH_TO_PERL -ne 'split /\t/; $a = $_[9]; $a =~ s/ \| /\|/g; @a = split /\|/, $a; shift @a; print if ( (join "", @a) ne (join "", sort @a));' $mapFile >! x.$$.txt
set ct = `cat x.$$.txt | wc -l`
if ($ct != 0) then
    echo "ERROR: map advice out of order"
    $PATH_TO_PERL -ne 'split /\t/; $a = $_[9]; $a =~ s/ \| /\|/g; @a = split /\|/, $a; shift @a; print if ( (join "", @a) ne (join "", sort @a));' $mapFile |\
       cut -f 1 | sed 's/^/      /'
endif
/bin/rm -f x.$$.txt


# End of "if rule based mapping"
endif

#
# Continue verifying $mapDescriptorFile
#

#
# Verify referencedComponentId is in $mapFile
#
echo "    Verify referencedComponentId in iissscc or c RefSet files ...`/bin/date`"
$PATH_TO_PERL -ne 'split /\t/; print "$_[5]\n";' $mapDescriptorFile | grep -v -i REFERENCED | sort -u -o x.txt
$PATH_TO_PERL -ne 'split /\t/; print "$_[4]\n";' $mapFile | grep -v -i refSetId | sort -u -o x2.txt
set ct = `comm -23 x.txt x2.txt | wc -l`
if ($ct > 0) then
    echo "ERROR: referencedComponentId in descriptor file is not refSetId in mapping files"
    comm -23 x.txt x2.txt | sed 's/^/      /'
endif
/bin/rm -f x.txt x2.txt

#
# Continue verifying $moduleDependencyFile
#

#
# Verify refSetId is the moduleId
#900000000000534007
echo "    Verify refSetId ss RefSet file is module id ...`/bin/date`"
set ct = `$PATH_TO_PERL -ne 'split /\t/; print "$_[4]\n" unless $_[4] eq "900000000000534007";' $moduleDependencyFile | grep -v -i refSetId | wc -l`
if ($ct > 0) then
    echo "ERROR: module dependency refSetId != moduleId ($moduleId)"
    $PATH_TO_PERL -ne 'split /\t/; print "$_[4]\n" unless $_[4] eq "900000000000534007";' $moduleDependencyFile | grep -v -i refSetId  |\
    sed 's/^/      /'
endif

#
# Verify moduleId is the moduleId from map file
#
echo "    Verify moduleId ss RefSet file is moduleId of map file ...`/bin/date`"
set moduleId = `$PATH_TO_PERL -ne 'split /\t/; if ($_[3] ne "moduleId") { print "$_[3]\n"; last; };' $mapFile`
set ct = `$PATH_TO_PERL -ne 'split /\t/; print "$_[3]\n" unless $_[3] eq "'$moduleId'";' $moduleDependencyFile | grep -v -i moduleId | wc -l`
if ($ct > 0) then
    echo "ERROR: module dependency moduleId != moduleId ($moduleId)"
    $PATH_TO_PERL -ne 'split /\t/; print "$_[3]\n" unless $_[3] eq "'$moduleId'";' $moduleDependencyFile | grep -v -i moduleId  |\
    sed 's/^/      /'
endif

#
# Verify referencedComponentId are the core and metadata concept ids
#  "900000000000207008" - core
#  "900000000000012004" - metadata
echo "    Verify referencedComponentId are the core and metadata concept ids ...`/bin/date`"
set ct = `$PATH_TO_PERL -ne 'split /\t/; print "$_[5]\n" unless ($_[5] eq "900000000000207008" || $_[5] eq "900000000000012004" || lc($_[5]) eq "referencedcomponentid");' $moduleDependencyFile | wc -l`
if ($ct > 0) then
    echo "ERROR: referencedComponentId in descriptor file is not refSetId in mapping files"
    comm -23 x.txt x2.txt | sed 's/^/      /'
endif
/bin/rm -f x.txt x2.txt

#
# Verify sourceEffectiveTime matches the version of the data
#
echo "    Verify sourceEffectiveTime matches the version of the data ...`/bin/date`"
set version = `echo $mapFile:t | $PATH_TO_PERL -pe 's/.*(\d{8}).*/$1/'`
set ct = `$PATH_TO_PERL -ne 'split /\t/; print unless $_[6] eq "'$version'";' $moduleDependencyFile | grep -v moduleId | wc -l`
if ($ct != 0) then
    echo "ERROR: sourceEffectiveTime should be set to $version (version of the map data)"
    $PATH_TO_PERL -ne 'split /\t/; print unless $_[6] eq "'$version'";' $moduleDependencyFile | sed 's/^/      /'
endif

#
# Verify targetEffectiveTime matches the version of the core data
#
echo "    Verify targetEffectiveTime matches the version of the core data ...`/bin/date`"
if ($coreDir != "") then
    set version = `echo $coreDir/*Concept*txt | $PATH_TO_PERL -pe 's/.*(\d{8}).*/$1/'`
    set coreModuleId = 900000000000207008
    set metadataModuleId = 900000000000012004
    set ct = `grep $coreModuleId $moduleDependencyFile | $PATH_TO_PERL -ne 'chop; chop; split /\t/; print "$_\n" unless $_[7] eq "'$version'";' | wc -l`
    if ($ct != 0) then
        echo "ERROR: targetEffectiveTime should be set to $version (version of the core data)"
        grep $coreModuleId $moduleDependencyFile | $PATH_TO_PERL -ne 'chop; chop; split /\t/; print "$_\n" unless $_[7] eq "'$version'";' | sed 's/^/      /'
    endif
    set version = `echo $mapFile:t | $PATH_TO_PERL -pe 's/.*(\d{8}).*/$1/'`
    set metadataModuleId = 900000000000012004
    set ct = `grep $metadataModuleId $moduleDependencyFile | $PATH_TO_PERL -ne 'chop; cho; split /\t/; print unless $_[7] eq "'$version'";' | wc -l`
    if ($ct != 0) then
        echo "ERROR: targetEffectiveTime should be set to $version (version of the map data)"
        grep $metadataModuleId $moduleDependencyFile | $PATH_TO_PERL -ne 'chop; chop; split /\t/; print unless $_[7] eq "'$version'";' | sed 's/^/      /'
    endif

else
    echo "      SKIP TEST: no -core flag specified"
endif

#
# SNOMEDCT to ICD10 Semantic Rules
#

#
# Verify all referencedComponentId are Clinical Finding, Event, or Situation
#
echo "    Verify all referencedComponentId are Clinical Finding, Event, or Situation ...`/bin/date`"
if ($coreDir != "") then
    # for RF2 this should be
    $MAPPING_HOME/bin/descHelper.pl $MAPPING_HOME/etc/scope.txt $coreDir/*_Relation*txt ID |\
        cut -d\| -f 2 | sort -u -o x2.txt
    $PATH_TO_PERL -ne 'split /\t/; print "$_[5]\n";' $mapFile | grep -v -i REFERENCED | sort -u -o x.txt
    set ct = `comm -23 x.txt x2.txt | wc -l`
    if ($ct > 0) then
        echo "ERROR: referencedComponentId not Clinical Finding, Event, or Situation"
        comm -23 x.txt x2.txt | sed 's/^/      /'
    endif
    /bin/rm -f x.txt x2.txt
else
    echo "      SKIP TEST: no -core flag specified"
endif

#
# Verify mapGroups with IFA rules are represented in canonical short form
#
# TODO: needs implementation


#
# Verify there are no mappings for non human concepts (referencedComponentId)
#
$PATH_TO_PERL -ne 'split /\t/; print "$_[5]\n";' $mapFile | sort -u |\
  comm -12 - $MAPPING_HOME/etc/nonHumanSubset.txt >&! x.txt
set ct = `cat x.txt | wc -l`
if ($ct > 0) then
    echo "ERROR: map records for non human concepts found"
    cat x.txt | sed 's/^/      /'
endif
/bin/rm -f x.txt


#
# Verify there are no IFA rules for non human concepts (mapRule)
#
$PATH_TO_PERL -ne 'split /\t/; $rule = $_[8]; if ($rule =~ /IFA /) { $rule =~ /IFA ([0-9]*) /; print "$1\n";}' $mapFile | sort -u |\
  comm -12 - $MAPPING_HOME/etc/nonHumanSubset.txt >&! x.txt
set ct = `cat x.txt | wc -l`
if ($ct > 0) then
    echo "ERROR: IFA rules for non human concepts found"
    cat x.txt | sed 's/^/      /'
endif
/bin/rm -f x.txt


echo "----------------------------------------------------------------------"
echo "Finished ... `/bin/date`"
echo "----------------------------------------------------------------------"
