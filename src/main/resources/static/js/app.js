

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
            var expiration = "";
            if(data.accessMap[dbName].readAccess) {
                if(data.accessMap[dbName].readExpires != undefined) {
                    expiration = data.accessMap[dbName].readExpires
                }
                newRow = newRow + "<td class='active'>"+expiration+"</td>";
            } else {
                newRow = newRow + "<td class='inactive'></td>";
            }
            // writer
            if(data.accessMap[dbName].writeAccess) {
                if(data.accessMap[dbName].writeExpires != undefined) {
                    expiration = data.accessMap[dbName].writeExpires
                }
                newRow = newRow + "<td class='active'>"+expiration+"</td>";
            } else {
                newRow = newRow + "<td class='inactive'></td>";
            }
            // admin
            if(data.accessMap[dbName].adminAccess) {
                if(data.accessMap[dbName].adminExpires != undefined) {
                    expiration = data.accessMap[dbName].adminExpires
                }
                newRow = newRow + "<td class='active'>"+expiration+"</td>";
            } else {
                newRow = newRow + "<td class='inactive'></td>";
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
                $(this).dialog("close");
                $.get( "access/requestAccess/"+dbName+"/reader", function( data ) {
                    console.log("Requested read access for " + dbName);
                    loadUserData();
                }).fail(function (data) {
                    alert("Could not grant you the requested access : "+data.responseText);
                });
            },
            "Write": function() {
                $(this).dialog("close");
                $.get( "access/requestAccess/"+dbName+"/writer", function( data ) {
                    console.log("Requested write access for " + dbName);
                    loadUserData();
                }).fail(function (data) {
                    alert("Could not grant you the requested access : "+data.responseText);
                });

            },
            "Admin": function() {
                $(this).dialog("close");
                $.get( "access/requestAccess/"+dbName+"/admin", function( data ) {
                    console.log("Requested admin access for " + dbName);
                    loadUserData();
                }).fail(function (data) {
                    alert("Could not grant you the requested access : "+data.responseText);
                });
            },
            "Cancel": function() { $(this).dialog("close"); }}
    });
}

console.log("Before load");

// start by loading the user data
loadUserData();
console.log("After load");