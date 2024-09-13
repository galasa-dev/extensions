/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.auth.couchdb.internal.beans;

//{
//   "_id": "_design/docs",
//   "_rev": "3-xxxxxxxxxxx9c9072dyy",
//   "views": {
//     "loginId-view": {
//       "map": "function (doc) {\n  if (doc.owner && doc.owner.loginId) {\n    emit(doc.owner.loginId, doc);\n  }\n}"
//     }
//   },
//   "language": "javascript"
// }    
public class TokensDBNameViewDesign {
    public String _rev;
    public String _id;
    public TokenDBViews views;
    public String language;
}
    