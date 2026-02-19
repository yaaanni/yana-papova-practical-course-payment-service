db = db.getSiblingDB("admin");

db.createUser({
  user: "yana",
  pwd: "1234",
  roles: [
    { role: "readWrite", db: "payments" },
    { role: "dbAdmin", db: "payments" }
  ]
});
