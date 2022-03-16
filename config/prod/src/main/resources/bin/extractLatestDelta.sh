
#!/bin/bash
# Script that takes a snomed hierarchy folder with Delta contents and keep only the most recent row per element
# parent_dir should contain the Delta directory and the Delta2 output directory will be created in the parent_dir during script execution
# usage:  ./createSctSnapshotFiles.sh parent_dir 
# example:  ./createSctSnapshotFiles.sh /cygdrive/d/SnomedCT_InternationalRF2_MEMBER_20220131T120000Z 



args=("$@")
parent_dir=${args[0]};
initial_dir=$parent_dir/Delta
final_dir=$parent_dir/Delta2

echo "------------------------------------------------------------"
echo "Starting .. `/bin/date`"
echo "Initial dir:         $initial_dir"
echo "Final dir:     $final_dir"
echo "------------------------------------------------------------"

# assume parent_dir exists and create Delta2 directory 
if [ -f $final_dir ]; then
  rm $final_dir
fi
mkdir $final_dir
if [ ! -f $final_dir/Terminology ]; then
  mkdir $final_dir/Terminology
  chmod 777 $final_dir/Terminology
fi



# one file at a time, process the delta files keeping only the most recent row per id and printing that row to the corresponding delta2 output file


cd $initial_dir/Terminology
f1=`find ~+ -type f -name 'sct2_TextDefinition*.txt'`
echo $f1
cd $final_dir
f2=`echo $f1 | sed -e 's/Delta/Delta2/'`
echo $f2
touch $f2

prevId=id
tac $f1 | awk '$1!='"$prevId"' {print $_;'"$prevId"'=$1}' | sed '1h;1d;$!H;$!d;G' > $f2




cd $initial_dir/Terminology
f1=`find ~+ -type f -name 'sct2_Relationship_*.txt'`
echo $f1
cd $final_dir
f2=`echo $f1 | sed -e 's/Delta/Delta2/'`
echo $f2
touch $f2

prevId=id
tac $f1 | awk '$1!='"$prevId"' {print $_;'"$prevId"'=$1}' | sed '1h;1d;$!H;$!d;G' > $f2



cd $initial_dir/Terminology
f1=`find ~+ -type f -name 'sct2_Description*.txt'`
echo $f1
cd $final_dir
f2=`echo $f1 | sed -e 's/Delta/Delta2/'`
echo $f2
touch $f2

prevId=id
tac $f1 | awk '$1!='"$prevId"' {print $_;'"$prevId"'=$1}' | sed '1h;1d;$!H;$!d;G' > $f2


cd $initial_dir/Terminology
f1=`find ~+ -type f -name 'sct2_Concept_*.txt'`
echo $f1
cd $final_dir
f2=`echo $f1 | sed -e 's/Delta/Delta2/'`
echo $f2
touch $f2

prevId=id
tac $f1 | awk '$1!='"$prevId"' {print $_;'"$prevId"'=$1}' | sed '1h;1d;$!H;$!d;G' > $f2


cd $initial_dir/Refset/Language
f1=`find ~+ -type f -name 'der2_cRefset_Language*.txt'`
echo $f1
cd $final_dir
f2=`echo $f1 | sed -e 's/Delta/Delta2/' | sed -e 's/Refset\/Language/Terminology/'`
echo $f2
touch $f2

prevId=id
tac $f1 | awk '$1!='"$prevId"' {print $_;'"$prevId"'=$1}' | sed '1h;1d;$!H;$!d;G' > $f2







echo "------------------------------------------------------------"
echo "Ending.. `/bin/date`"
echo "------------------------------------------------------------"
