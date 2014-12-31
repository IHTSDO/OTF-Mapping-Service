#!/bin/tcsh -f
#
# Converts the map file to RF1
#
set usage = "Usage: $0 <RF1 dir> <RF2 snapshot map file>"
#
# Set environment (if configured)
#

#
# Check required environment variables
#


#
# Parse arguments
#
if ($#argv == 0) then
    echo "Error: Incorrect number of arguments: 0"
    echo "$usage"
    exit 1
endif

set i = 1
set svn = 0
set quick = 0
set deleteFlag = 0
while ($i <= $#argv)
    switch ($argv[$i])
        case '-*help':

            cat << EOF

 Takes the previous version RF1 ICD9 map dir and the snapshot RF2 file
 and creates new version RF1 files in the current directory.

  $usage

 Parameters:
    <RF1 dir>        : prev RF1 dir, e.g. /project/snomed/20130731/SnomedCT_Release_INT_20130731/RF1Release/Maps/ICD9/
    <mapFile>        : the RF2 snapshot map file

 Options:
    -h help          : On-line help

EOF
            exit 0
        default :
            set arg_count=2
            set all_args=`expr $i + $arg_count - 1`
            if ($all_args != $#argv) then
                echo "Error: Incorrect number of arguments: $all_args, $#argv"
                echo "$usage"
                exit 1
            endif
            set rf1MapDir=$argv[$i]
            @ i = $i + 1
            set mapFile=$argv[$i]
    endsw
    @ i = $i + 1
end

#
# Begin program logic
#
echo "----------------------------------------------"
echo "Starting $0 ... `/bin/date`"
echo "----------------------------------------------"
echo "RF1 dir:     $rf1MapDir"
echo "Map file:    $mapFile"
echo "   (snapshot)"
echo ""

#
# Determine RF2 version
#
echo "    Determine version ...`/bin/date`"
set version = `echo $mapFile:t | $PATH_TO_PERL -pe 's/.*(\d{8}).*/$1/'`
echo "      version  = $version"

#
# Set filenames
#
echo "    Set filenames ...`/bin/date`"
set crossMapSetsFile = der1_CrossMapSets_ICD9_INT_$version.txt
set crossMapsFile = der1_CrossMaps_ICD9_INT_$version.txt
set crossMapTargetsFile = der1_CrossMapTargets_ICD9_INT_$version.txt

#
# Create new crossmapsets file from the old one
# TODO: determine whether any fields need to be updated - likely not
#
echo "    Create $crossMapSetsFile ...`/bin/date`"
cp $rf1MapDir/der1_CrossMapSets_ICD9_INT_*.txt $crossMapSetsFile

#
# Determine map targets that require new target ids
#  (save UUID=>targetid map in $MAPPING_HOME/etc)
#
echo "    Determine which map targets require new identifiers ...`/bin/date`"

# get targetid, targetcodes from previous RF1
$PATH_TO_PERL -ne 'split /\t/; print "$_[2]\t$_[0]\n"' $rf1MapDir/der1_CrossMapTargets_ICD9_INT_*.txt | sort -u -o targetIds.$$.txt
cut -f 1 targetIds.$$.txt | sort -u -o targetsWithIds.$$.txt

($PATH_TO_PERL -ne 'split /\t/; print if $_[2] == 1;' $mapFile; echo "") |\
  cut -f 6,7,8,11 | $PATH_TO_PERL -ne 'chop; ($cid,$group,$priority,$target) = split /\t/; \
  if ($prevCid && $cid ne $prevCid) { \
    print (join "|", @targets); print "\n"; @targets=(); } \
  $prevCid = $cid; $prevGroup = $group; push @targets, $target;' | sort -u |\
  comm -23 - targetsWithIds.$$.txt | sort -u -o targetsWithoutIds.$$.txt

# make UUID file (UUID|targets)
$PATH_TO_PERL -ne 'BEGIN { use lib "$ENV{MAPPING_HOME}/lib"; use UUID::Tiny; binmode(STDIN, ":raw:encoding(UTF-8)"); } \
  chop; $uuid = UUID::Tiny::UUID_to_string(UUID::Tiny::create_UUID(5, "$_")); \
  print "$uuid\t$_\n";' targetsWithoutIds.$$.txt >! targetUuidsWithoutIds.$$.txt

#
# Generate new ids for "targets without ids"
#
echo "    Generate new identifiers ...`/bin/date`"
mkdir x$$
cat >! x$$/pom.xml <<EOF
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <!-- Module Metadata -->
  <groupId>org.ihtsdo</groupId>
  <artifactId>ihtsdo-map-id-module</artifactId>
  <version>0.0.1</version>
  <!-- JPA dependencies -->
  <dependencies>
    <dependency>
      <groupId>org.ihtsdo</groupId>
      <artifactId>id-generation-api</artifactId>
      <version>2.26-trek-no-jini</version>
    </dependency>
    <dependency>
      <groupId>junit</groupId>
      <artifactId>junit</artifactId>
      <version>4.8.2</version>
      <scope>test</scope>
    </dependency>
  </dependencies>
</project>
EOF
pushd x$$ >>& /dev/null
mkdir src
mkdir src/test
mkdir src/test/java
cat >! src/test/java/AssignTest.java <<EOF
import java.io.*;
import java.util.UUID;
import org.ihtsdo.idgeneration.IdAssignmentBI;
import org.ihtsdo.idgeneration.IdAssignmentImpl;
import org.junit.Test;
public class AssignTest {
 @Test
  public void test() throws Exception {
    IdAssignmentBI idAssignment =
      new IdAssignmentImpl(
        "http://mgr.servers.aceworkspace.net:50042/axis2/services/id_generator",
        "termmed", "snomed");
    BufferedReader in = new BufferedReader(
        new FileReader(new File("../targetUuidsWithoutIds.$$.txt")));
    PrintWriter out = new PrintWriter(
        new FileWriter(new File("../targetUuidIds.$$.txt")));
    String line = null;
    while ((line = in.readLine()) != null) {
        final String uuidStr = line.substring(0,line.indexOf('\t'));
        final String targetCodes = line.substring(line.indexOf('\t')+1);
        System.out.println("      processing " + uuidStr + ", " + targetCodes);

        UUID componentUuid = UUID.fromString(uuidStr);
        Long newSctId =
          idAssignment.createSCTID(componentUuid, 0, "05", "$version",
              "$version", "12345");
        out.println(targetCodes + "\t" + uuidStr + "\t" + newSctId);
     }
     out.close();
  }
}
EOF

$PATH_TO_MVN clean test >&! /tmp/mvn.$$.log
if ($status != 0) then
    echo "ERROR running maven (/tmp/mvn.$$.log)"
    exit 1
endif
popd >>& /dev/null
/bin/rm -rf x$$

#
# Add to targetsWithIds.$$.txt
#
echo "    Collect all assigned targets ...`/bin/date`"
sort -u -o targetsWithoutIds.$$.txt{,}
sort -t\         -k 1,1 targetUuidIds.$$.txt | join -t\  -j 1 -o 1.1 2.3 targetsWithoutIds.$$.txt - |\
  sort >> targetIds.$$.txt
sort -u -o targetIds.$$.txt targetIds.$$.txt
sort -t\         -k 1,1 -o targetIds.$$.txt targetIds.$$.txt

#
# Archive UUID map
#
mv targetUuidIds.$$.txt $MAPPING_HOME/etc/rf1UuidTargetIdMap.txt

#
# Create crossMapTargets file from active snapshot records
#
echo "    Build $crossMapTargetsFile ...`/bin/date`"
head -1 $rf1MapDir/der1_CrossMapTargets_ICD9_INT_*txt >! $crossMapTargetsFile

($PATH_TO_PERL -ne 'split /\t/; print if $_[2] == 1;' $mapFile; echo "") |\
  cut -f 6,7,8,11 | $PATH_TO_PERL -ne 'chop; ($cid,$group,$priority,$target) = split /\t/; \
  if ($prevCid && $cid ne $prevCid) { \
    print (join "|", @targets); print "\t$prevCid\n"; @targets = (); } \
  $prevCid = $cid; $prevGroup = $group; push @targets, $target;' | sort -u |\
  sort -t\       -k 1,1 |\
  join -t\        -j 1 -o 1.1 2.2 - targetIds.$$.txt |\
  $PATH_TO_PERL -ne 'chop; split /\t/; print "$_[1]\t2.16.840.1.113883.6.5.2.1\t$_[0]\t\t\r\n";' |\
  sort -u >> $crossMapTargetsFile

#
# Create crossMaps file from active snapshot records
#
echo "    Build $crossMapsFile ...`/bin/date`"
head -1 $rf1MapDir/der1_CrossMaps_ICD9_INT_*txt >! $crossMapsFile

($PATH_TO_PERL -ne 'split /\t/; print if $_[2] == 1;' $mapFile; echo "") |\
  cut -f 6,7,8,11,12 | $PATH_TO_PERL -ne 'chop; ($cid,$group,$priority,$target,$corrId) = split /\t/; \
  if ($prevCid && $cid ne $prevCid) { \
    print (join "|", @targets); print "\t$prevCid\t$prevCorrId\n"; @targets=(); } \
  $prevCid = $cid; $prevGroup = $group; push @targets, $target; $prevCorrId = $corrId' | sort -u |\
  sort -t\       -k 1,1 |\
  join -t\        -j 1 -o 2.2 1.2 1.3 - targetIds.$$.txt |\
  $PATH_TO_PERL -ne 'BEGIN { $x{447556008}="0"; $x{447557004}="1"; $x{447558009}="2"; $x{447559001}="3"; $x{447560006}="4"; } \
    chop; chop; split /\t/; print "100046\t$_[1]\t0\t0\t$_[0]\t\t$x{$_[2]}\r\n"' |\
  sort -u >> $crossMapsFile

#
# Reporting
#
echo "    Reporting ...`/bin/date`"
sort -u -o a $rf1MapDir/*CrossMaps*
sort -u -o b $crossMapsFile
echo "      Cross Map Records in common: "`comm -12 a b | wc -l`
echo "      Cross Map Records prev not current:"`comm -23 a b | wc -l`
echo "      Cross Map Records current not prev:"`comm -13 a b | wc -l`

sort -u -o a $rf1MapDir/*CrossMapTargets*
sort -u -o b $crossMapTargetsFile
echo "      Cross Map Targets in common: "`comm -12 a b | wc -l`
echo "      Cross Map Targets prev not current:"`comm -23 a b | wc -l`
echo "      Cross Map Targets current not prev:"`comm -13 a b | wc -l`
/bin/rm -f a b

#
# Cleanup
#
echo "    Cleanup ...`/bin/date`"
/bin/rm -f targetIds.$$.txt targetsWithIIds.$$.txt targetUuidsWithoutIds.4553.txt

echo ""
echo "----------------------------------------------"
echo "Finished $0 ... `/bin/date`"
echo "----------------------------------------------"
