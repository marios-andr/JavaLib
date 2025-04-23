FILE_PATH="$1"

# URL encode function in pure bash
raw_urlencode() {
    local string="$1"
    local length="${#string}"
    local encoded=""

    for (( i = 0; i < length; i++ )); do
        c="${string:$i:1}"
        case "$c" in
            [a-zA-Z0-9.~_-]) encoded+="$c" ;;
            ' ') encoded+="%20" ;;
            *) printf -v hex '%%%02X' "'$c"
               encoded+="$hex"
               ;;
        esac
    done

    echo "$encoded"
}

# Encode the file path
ENCODED=$(raw_urlencode "$FILE_PATH")

# Call curl with encoded URL
curl "http://localhost:8080?filepath=$ENCODED"