

function loadUserData() {
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
        // first clear the table
        $(".tableContent").remove();

        for(var i=0;i<dbList.length;i++) {
            dbName = dbList[i];
            var newRow = '<tr class="tableContent"><td>'+dbName+'</td>';
            // reader
            if(data.accessMap[dbName].readAccess) {
                newRow = newRow + "<td bgcolor='green'></td>";
            } else {
                newRow = newRow + "<td bgcolor='#8b0000'></td>";
            }
            // writer
            if(data.accessMap[dbName].writeAccess) {
                newRow = newRow + "<td bgcolor='green'></td>";
            } else {
                newRow = newRow + "<td bgcolor='#8b0000'></td>";
            }
            // admin
            if(data.accessMap[dbName].adminAccess) {
                newRow = newRow + "<td bgcolor='green'></td>";
            } else {
                newRow = newRow + "<td bgcolor='#8b0000'></td>";
            }
            // actions
            newRow = newRow +"<td align='center'><a onclick=\"requestAccess(\'"+dbName+"\');\">Request access</a></td>";
            newRow = newRow +"</tr>";
            $('#accessTable tr:last').after(newRow);
        }
    });
}

function requestAccess(dbName) {
    var diag = $('#dialog').dialog({
        autoResize: true,
        show: "clip",
        hide: "clip",
        height: 'auto',
        width: 'auto',
        autoOpen: true,
        modal: true,
        //position: 'center',
        draggable: true,

        open: function (type, data) {
            console.log("Request access for db "+dbName);
        },

        buttons: {  "Read": function() {
                $.get( "access/requestAccess/"+dbName+"/reader", function( data ) {
                    console.log("Requested read access for " + dbName);
                    loadUserData();
                });
                $(this).dialog("close");
            },
            "Write": function() {
                $.get( "access/requestAccess/"+dbName+"/writer", function( data ) {
                    console.log("Requested write access for " + dbName);
                    loadUserData();
                });
                $(this).dialog("close");
            },
            "Admin": function() {
                $.get( "access/requestAccess/"+dbName+"/admin", function( data ) {
                    console.log("Requested admin access for " + dbName);
                    loadUserData();
                });
                $(this).dialog("close");
            },
            "Cancel": function() { $(this).dialog("close"); }}
    });
}

console.log("Before load");

// start by loading the user data
loadUserData();
console.log("After load");