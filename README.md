# CouchDB access manager

## The problem
Say you have a couchDB installation with several databases and you want to give access to team members, maybe even for a limited
time... but your user management is put into an IDP like keycloak. You don't want to give curl to your user management team, but
you cannot plug keycloak directly into couch... and even then, you would still need to setup the roles.

So this access manager tries to make your life easier, creating some sensible defaults.

## The security model translation
A main goal is that (as an admin)you can inspect what the access manager did via fauxton. You can also use the available user management
screens to add/remove access.. the access manager will take those modifications into account.

The access manager does not use generic roles to grant access, but specific users.. this way you can directly see who currently has access in the
fauxton permission view. So an admin is added to the admin list, a reader/writer to the members list.

A writer is a member that has an additional role. This means that we do not support writers that cannot read (which would not make that much
sense anyway). So retracting or limiting the reader access, directly retracts the writer permission as well.

## Features

### Activate security
When there is no ```_users``` database in your cluster, it creates it. For each database, when there is no ```_security``` document,
it creates it

### Keycloak authentication/roles
The user accessing the access manager will need to provide a valid access token. So if you disable the user in keycloak, he can
no longer request access. Combined with the temporary access, you effectively get your users locked out of couchDB when  you
remove the roles from keycloak.

At the moment, 3 roles are defined :
* couch_admin : allows a user to request admin access to a database
* couch_reader : allows a user to request read access to a database
* couch_writer : allows a user to request write access to a database

In combination with the ```couch_[admin/reader/writer]_dbs``` attribute, you can restrict on which databases a user can request access.
It is a comma separated list of databases you will allow this kind of access to. If not present, the
user can request access to all databases

**Make sure to create a proper role mapping for the client so that this attribute is present in your access token!**


### Temporary access
Passwords are nice, but you do not always want to grant permanent access. Therefore the access manager keeps track of the 
access it grants, and revokes it when the duration expires.

The governing attribute is ```couch_[admin/reader/writer]_duration```. This is the number of minutes the access will remain active. If
not present, it defaults to 1440 (so 24 hours). If you want to disable expiration, set the attribute to -1

**Make sure to create a proper role mapping for the client so that this attribute is present in your access token!**

**At the moment, there is no support for different expiration time per database**

### Read-only access
The access manager is capable of installing a validation document to effectively create a read-only access.


### Justification
The access manager keeps a track record of which access is requested, and allows the user to justify that access. This allows
your cluster admin to look not only who had access, but for what this access was supposed to be used... up to you to check
the access logs to see whether it fits.
