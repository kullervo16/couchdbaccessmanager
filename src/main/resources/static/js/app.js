

console.log("Before load");

// start by loading the user data
$.get( "access/userData", function( data ) {
    console.log(data)
    $("#fetching").hide();
    $("#userId").text(data.userId);
    $("#userName").text(data.userName);
    $("#userData").show();
    if(data.newUser) {
        $("#password").text(data.password);
        $("#newPassword").show();
    }
    var dbList = [];
    for(var i=0;i<Object.keys(data.accessMap).length;i++) {
        var dbName = Object.keys(data.accessMap)[i];
        if(dbName.charAt(0) !== '_') {
            dbList.push(dbName);
        }
    }
    dbList = dbList.sort();
    console.log(dbList);
    for(var i=0;i<dbList.length;i++) {
        dbName = dbList[i];
        var newRow = '<tr><td>'+dbName+'</td>';
        // reader
        if(data.accessMap[dbName].readAccess) {
            newRow = newRow + "<td bgcolor='green'></td>";
        } else {
            newRow = newRow + "<td bgcolor='red'></td>";
        }
        // writer
        if(data.accessMap[dbName].writeAccess) {
            newRow = newRow + "<td bgcolor='green'></td>";
        } else {
            newRow = newRow + "<td bgcolor='red'></td>";
        }
        // admin
        if(data.accessMap[dbName].adminAccess) {
            newRow = newRow + "<td bgcolor='green'></td>";
        } else {
            newRow = newRow + "<td bgcolor='red'></td>";
        }
        // actions
        newRow = newRow +"<td></td>";
        newRow = newRow +"</tr>";
        $('#accessTable tr:last').after(newRow);
    }
});
console.log("After load");