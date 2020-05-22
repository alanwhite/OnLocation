#!/bin/bash

echo "Convert All Icons8 100px png's to required resolutions"

IMG="/c/Progra~1/ImageMagick-7.0.9-Q16/magick.exe"

# $IMG -usage

declare -A names=(
	["icons8-compress"]="icons8-compress"
	["icons8-enlarge"]="icons8-enlarge"
	["icons8-expand"]="icons8-expand"
	["icons8-original-size"]="icons8-original-size"
	["icons8-record"]="icons8-record"
	["icons8-stop"]="icons8-stop"
)

cd icons
for f in *.png; do
	for size in 24 30 32 36 40 48 64; do
		f1=${f%%-100*}
		echo "$f to ./res/${names[$f1]}-$size.png"
		eval "$IMG convert $f -resize ${size}x${size} ../res/${names[$f1]}-${size}.png" 
	done
done

