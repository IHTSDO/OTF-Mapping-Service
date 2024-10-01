#!/bin/bash

OUTPUT_FILE_CONCEPTS=$2/concepts.txt
OUTPUT_FILE_PAR_CHILD=$2/parent-child.txt
OUTPUT_FILE_ATTRIBUTE=$2/concept-attributes.txt

rm $OUTPUT_FILE_CONCEPTS

awk -F, 'BEGIN{FS="$";OFS="|";}  {print $1, $2 }' $1/soc.asc >> $OUTPUT_FILE_CONCEPTS
awk -F, 'BEGIN{FS="$";OFS="|";}  {print $1, $2 }' $1/hlgt.asc >> $OUTPUT_FILE_CONCEPTS
awk -F, 'BEGIN{FS="$";OFS="|";}  {print $1, $2 }' $1/hlt.asc >> $OUTPUT_FILE_CONCEPTS
awk -F, 'BEGIN{FS="$";OFS="|";}  {print $1, $2 }' $1/pt.asc >> $OUTPUT_FILE_CONCEPTS
#awk -F, 'BEGIN{FS="$";OFS="|";}  { sub (/\$Y\$/, "$-NC$"); } { sub (/\$N\$/, "$$"); } { if ($1!=$3) print $1, $2$10 }' $1/llt.asc >> $OUTPUT_FILE_CONCEPTS
awk -F, 'BEGIN{FS="$";OFS="|";}  $1!=$3{ if ($10=="Y") print $1, $2 }' $1/llt.asc >> $OUTPUT_FILE_CONCEPTS

rm $OUTPUT_FILE_PAR_CHILD

awk -F, 'BEGIN{FS="$";OFS="|";} {print $1, $2 }' $1/soc_hlgt.asc >> $OUTPUT_FILE_PAR_CHILD
awk -F, 'BEGIN{FS="$";OFS="|";} {print "root", $1}' $1/soc.asc >> $OUTPUT_FILE_PAR_CHILD
awk -F, 'BEGIN{FS="$";OFS="|";} {print $1, $2 }' $1/hlgt_hlt.asc >> $OUTPUT_FILE_PAR_CHILD
awk -F, 'BEGIN{FS="$";OFS="|";} {print $1, $2 }' $1/hlt_pt.asc >> $OUTPUT_FILE_PAR_CHILD
awk -F, 'BEGIN{FS="$";OFS="|";} $1!=$3{ if ($10=="Y") print $3, $1 }' $1/llt.asc >> $OUTPUT_FILE_PAR_CHILD

rm $OUTPUT_FILE_ATTRIBUTE
echo Currency>> $OUTPUT_FILE_ATTRIBUTE
echo Concept Type>> $OUTPUT_FILE_ATTRIBUTE
echo Preferred Path>> $OUTPUT_FILE_ATTRIBUTE
awk -F, 'BEGIN{FS="$";OFS="|";} {print $1, "Concept Type|SOC" }' $1/soc.asc >> $OUTPUT_FILE_ATTRIBUTE
awk -F, 'BEGIN{FS="$";OFS="|";} {print $1, "Concept Type|HLGT" }' $1/hlgt.asc >> $OUTPUT_FILE_ATTRIBUTE
awk -F, 'BEGIN{FS="$";OFS="|";} {print $1, "Concept Type|HLT" }' $1/hlt.asc >> $OUTPUT_FILE_ATTRIBUTE
awk -F, 'BEGIN{FS="$";OFS="|";} {print $1, "Concept Type|PT" }' $1/pt.asc >> $OUTPUT_FILE_ATTRIBUTE
#awk -F, 'BEGIN{FS="$";OFS="|";} {print $1, "Concept Type|LLT" }' $1/llt.asc >> $OUTPUT_FILE_ATTRIBUTE
awk -F, 'BEGIN{FS="$";OFS="|";} { if ($10=="Y") print $1, "Concept Type|LLT" }' $1/llt.asc >> $OUTPUT_FILE_ATTRIBUTE
awk -F, 'BEGIN{FS="$";OFS="|";} { if ($12=="Y") print $1, "Preferred Path|"$4"--"$3"--"$2"--"$1 }' $1/mdhier.asc >> $OUTPUT_FILE_ATTRIBUTE
awk -F, 'BEGIN{FS="$";OFS="|";} $1!=$3{ if ($10=="Y") print $1,"Currency",$10 }' $1/llt.asc >> $OUTPUT_FILE_ATTRIBUTE