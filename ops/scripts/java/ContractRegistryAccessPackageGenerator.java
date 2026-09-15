import com.healthmarketscience.jackcess.ColumnBuilder;
import com.healthmarketscience.jackcess.DataType;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.IndexBuilder;
import com.healthmarketscience.jackcess.RelationshipBuilder;
import com.healthmarketscience.jackcess.Table;
import com.healthmarketscience.jackcess.TableBuilder;

import java.io.File;
import java.sql.*;
import java.util.*;

/**
 * Contract-driven empty Access package generator. It never contains a KIDS
 * table list or field list: both are read from platform.contract_* registry.
 * Usage: host database user password contractRevision output.accdb
 */
public final class ContractRegistryAccessPackageGenerator {
    private ContractRegistryAccessPackageGenerator() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 6) throw new IllegalArgumentException("Usage: host database user password contractRevision output.accdb");
        String host=args[0], dbName=args[1], user=args[2], password=args[3];
        int revision=Integer.parseInt(args[4]); File output=new File(args[5]);
        if (output.exists()) throw new IllegalStateException("Refusing to overwrite existing file: "+output.getAbsolutePath());
        String url="jdbc:sqlserver://"+host+";databaseName="+dbName+";encrypt=true;trustServerCertificate=true";
        try(Connection c=DriverManager.getConnection(url,user,password); Database access=DatabaseBuilder.create(Database.FileFormat.V2010,output)) {
            List<TableSpec> specs=loadTables(c,revision); if(specs.isEmpty()) throw new IllegalStateException("No approved contract tables for revision "+revision);
            for(TableSpec spec:specs) createTable(access,spec);
            List<IndexSpec> indexes=loadIndexes(c,revision);
            for(IndexSpec index:indexes) createIndex(access,index);
            List<RelationSpec> relations=loadRelations(c,revision);
            for(RelationSpec relation:relations) createRelation(access,relation);
            populateMetadata(c,access,specs,indexes,relations,revision);
            System.out.println("Created contract-driven empty Access package: "+output.getAbsolutePath()+" tables="+specs.size()+" relations="+relations.size()+" metadata=declared");
        }
    }

    private static List<IndexSpec> loadIndexes(Connection c,int revision)throws Exception {
        List<IndexSpec> result=new ArrayList<>();
        String sql="WITH td AS (SELECT table_definition_id,logical_table_code,physical_table_name,revision,ROW_NUMBER() OVER(PARTITION BY physical_table_name ORDER BY revision DESC,table_definition_id DESC) rn FROM platform.contract_table_definition WHERE revision IN (1,?)) " +
                "SELECT td.physical_table_name,i.index_code,i.index_kind,i.field_list,i.is_unique FROM platform.contract_index_definition i JOIN td ON td.table_definition_id=i.table_definition_id AND td.rn=1 WHERE i.lifecycle_status='APPROVED' ORDER BY td.physical_table_name,i.index_code";
        try(PreparedStatement p=c.prepareStatement(sql)){p.setInt(1,revision);try(ResultSet rs=p.executeQuery()){while(rs.next())result.add(new IndexSpec(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getBoolean(5)));}}
        return result;
    }

    private static void createIndex(Database db,IndexSpec index)throws Exception {
        if("PRIMARY".equalsIgnoreCase(index.kind)) return; // primary key was created with the table
        Table table=db.getTable(index.tableName); if(table==null) return;
        String[] fields=splitKey(index.fieldList); if(fields.length==0)return;
        IndexBuilder builder=new IndexBuilder(index.code).addColumns(fields); if(index.unique)builder.setUnique(); builder.addToTable(table);
    }

    private static void populateMetadata(Connection c,Database db,List<TableSpec> specs,List<IndexSpec> indexes,List<RelationSpec> relations,int revision)throws Exception {
        db.getTable("__gs_package").addRow((Object[]) queryContractHeader(c,revision));
        for(TableSpec s:specs){
            db.getTable("__gs_dataset").addRow(s.logicalCode,s.accessName,s.family,s.grain);
            for(FieldSpec f:s.fields) db.getTable("__gs_field").addRow(s.logicalCode,f.name,f.logicalType,f.semanticRole,f.required?"true":"false");
            String[] key=splitKey(s.primaryKey); for(int i=0;i<key.length;i++) db.getTable("__gs_key").addRow(s.logicalCode,key[i],"PRIMARY",String.valueOf(i+1));
        }
        for(IndexSpec index:indexes){String logical=specs.stream().filter(s->s.accessName.equals(index.tableName)).map(TableSpec::logicalCode).findFirst().orElse(index.tableName);String[] fields=splitKey(index.fieldList);for(int i=0;i<fields.length;i++)if(!"PRIMARY".equalsIgnoreCase(index.kind))db.getTable("__gs_key").addRow(logical,fields[i],index.code,String.valueOf(i+1));}
        for(RelationSpec r:relations) db.getTable("__gs_relation").addRow(r.name,r.foreignTable,r.foreignField,r.primaryTable,r.primaryField,r.cardinality,r.required?"true":"false");
        if (db.getTable("__gs_structure_index") != null) loadStructureIndex(c,db,revision);
        loadClassifierMetadata(c,db); loadStatUnits(c,db);
    }

    private static void loadStructureIndex(Connection c,Database db,int revision)throws Exception {
        String sql="WITH ranked AS (SELECT s.*,ROW_NUMBER() OVER(PARTITION BY s.structure_code ORDER BY s.revision DESC,s.structure_id DESC) rn FROM platform.contract_structure s WHERE s.revision IN (1,?) AND s.lifecycle_status IN ('APPROVED','PROVISIONAL_APPROVED')) SELECT n.namespace_code,s.structure_code,s.structure_kind,s.data_class,s.grain,s.authority_mode,s.lifecycle_policy,s.lifecycle_status,s.revision,s.checksum FROM ranked s JOIN platform.contract_namespace n ON n.namespace_id=s.namespace_id WHERE s.rn=1 ORDER BY n.namespace_code,s.structure_code";
        try(PreparedStatement p=c.prepareStatement(sql)){p.setInt(1,revision);try(ResultSet r=p.executeQuery()){while(r.next())db.getTable("__gs_structure_index").addRow(r.getString(2),r.getString(1),r.getString(3),r.getString(4),r.getString(5),r.getString(6),r.getString(7),r.getString(8),r.getInt(9),r.getString(10));}}
    }

    private static String[] queryContractHeader(Connection c,int revision)throws Exception {
        String sql="SELECT TOP 1 p.product_code,ic.contract_code,CONVERT(varchar(20),ic.contract_revision),CONCAT(LOWER(p.product_code),'-contract-r',ic.contract_revision),CONCAT('r',ic.contract_revision),'EMPTY',N'KIDS_LEGACY_ACCESS',ic.format_profile FROM platform.ingestion_contract ic JOIN platform.dataset d ON d.dataset_id=ic.dataset_id JOIN platform.data_product p ON p.product_id=d.product_id WHERE ic.contract_revision=? ORDER BY ic.contract_id";
        try(PreparedStatement p=c.prepareStatement(sql)){p.setInt(1,revision);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalStateException("No issued ingestion contract revision "+revision);String[] out=new String[8];for(int i=0;i<8;i++)out[i]=r.getString(i+1);return out;}}
    }

    private static void loadClassifierMetadata(Connection c,Database db)throws Exception {
        try(Statement st=c.createStatement();ResultSet rs=st.executeQuery("SELECT scheme_code,title_ka,standard_reference FROM platform.classification_scheme ORDER BY scheme_code")){while(rs.next())db.getTable("__cl_scheme").addRow(rs.getString(1),"AUTHORITATIVE",rs.getString(2),rs.getString(3));}
        try(Statement st=c.createStatement();ResultSet rs=st.executeQuery("SELECT CONCAT(s.scheme_code,'|',v.version),s.scheme_code,v.version,v.status,v.valid_from,v.valid_to FROM platform.classification_version v JOIN platform.classification_scheme s ON s.scheme_id=v.scheme_id ORDER BY s.scheme_code,v.version")){while(rs.next())db.getTable("__cl_version").addRow(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getDate(5),rs.getDate(6));}
        try(Statement st=c.createStatement();ResultSet rs=st.executeQuery("SELECT CONCAT(s.scheme_code,'|',v.version,'|',i.code),CONCAT(s.scheme_code,'|',v.version),i.code,i.label_ka,i.label_en,i.status,i.sort_order FROM platform.classification_item i JOIN platform.classification_version v ON v.classification_version_id=i.classification_version_id JOIN platform.classification_scheme s ON s.scheme_id=v.scheme_id ORDER BY s.scheme_code,v.version,i.code")){while(rs.next())db.getTable("__cl_item").addRow(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getObject(7));}
    }

    private static void loadStatUnits(Connection c,Database db)throws Exception {
        try(Statement st=c.createStatement();ResultSet rs=st.executeQuery("SELECT unit_code,quantity_kind,scale_factor,denominator_text,title FROM platform.statistical_unit ORDER BY unit_code")){while(rs.next())db.getTable("__stat_unit").addRow(rs.getString(1),rs.getString(2),rs.getDouble(3),rs.getString(4),rs.getString(5));}
    }

    private static List<RelationSpec> loadRelations(Connection c,int revision)throws Exception {
        List<RelationSpec> result=new ArrayList<>();
        String sql="WITH td AS (SELECT logical_table_code,physical_table_name,ROW_NUMBER() OVER(PARTITION BY logical_table_code ORDER BY revision DESC,table_definition_id DESC) rn FROM platform.contract_table_definition WHERE revision IN (1,?)) " +
                "SELECT r.from_table_code,r.from_field_name,ft.physical_table_name,r.to_table_code,r.to_field_name,tt.physical_table_name,r.relation_code,r.cardinality,r.required " +
                "FROM platform.contract_structure_relation r JOIN td ft ON ft.logical_table_code=r.from_table_code AND ft.rn=1 JOIN td tt ON tt.logical_table_code=r.to_table_code AND tt.rn=1 " +
                "WHERE r.lifecycle_status IN ('APPROVED','PROVISIONAL_APPROVED') ORDER BY r.load_order,r.relation_definition_id";
        try(PreparedStatement p=c.prepareStatement(sql)){p.setInt(1,revision);try(ResultSet rs=p.executeQuery()){while(rs.next()) result.add(new RelationSpec(rs.getString(6),rs.getString(5),rs.getString(3),rs.getString(2),rs.getString(7),rs.getString(8),rs.getBoolean(9)));}}
        return result;
    }

    private static void createRelation(Database db,RelationSpec relation)throws Exception {
        if(db.getTable(relation.primaryTable)==null || db.getTable(relation.foreignTable)==null) return;
        new RelationshipBuilder(relation.primaryTable,relation.foreignTable).addColumns(relation.primaryField,relation.foreignField).setName(relation.name).setReferentialIntegrity().toRelationship(db);
    }

    private static List<TableSpec> loadTables(Connection c,int revision)throws Exception {
        List<TableSpec> result=new ArrayList<>();
        String sql="WITH ranked AS (SELECT td.table_definition_id,td.logical_table_code,COALESCE(td.access_table_name,td.physical_table_name) access_name,td.primary_key_expression,td.load_order,s.data_class,s.grain,td.revision,ROW_NUMBER() OVER(PARTITION BY COALESCE(td.access_table_name,td.physical_table_name) ORDER BY td.revision DESC,td.table_definition_id DESC) rn " +
                "FROM platform.contract_table_definition td JOIN platform.contract_structure s ON s.structure_id=td.structure_id " +
                "WHERE td.revision IN (1,?) AND s.lifecycle_status IN ('APPROVED','PROVISIONAL_APPROVED')) " +
                "SELECT table_definition_id,logical_table_code,access_name,primary_key_expression,load_order,data_class,grain FROM ranked WHERE rn=1 ORDER BY load_order,table_definition_id";
        try(PreparedStatement p=c.prepareStatement(sql)){p.setInt(1,revision);try(ResultSet r=p.executeQuery()){while(r.next()){
            TableSpec spec=new TableSpec(r.getLong(1),r.getString(2),r.getString(3),r.getString(4),r.getString(6),r.getString(7),new ArrayList<>()); loadFields(c,spec,revision); result.add(spec);
        }}}
        Map<String,TableSpec> byLogical=new LinkedHashMap<>();
        for(TableSpec s:result){TableSpec prior=byLogical.get(s.logicalCode);if(prior==null || (s.accessName.startsWith("__") && !prior.accessName.startsWith("__")))byLogical.put(s.logicalCode,s);}
        Map<String,TableSpec> byPhysical=new LinkedHashMap<>();
        for(TableSpec s:byLogical.values())byPhysical.putIfAbsent(s.accessName,s);
        return new ArrayList<>(byPhysical.values());
    }

    private static void loadFields(Connection c,TableSpec spec,int revision)throws Exception {
        String sql="SELECT field_name,logical_type,physical_type,semantic_role,required FROM platform.contract_field_definition WHERE table_definition_id=? AND lifecycle_status IN ('APPROVED','PROVISIONAL_APPROVED') ORDER BY ordinal";
        try(PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,spec.id);try(ResultSet r=p.executeQuery()){while(r.next())spec.fields.add(new FieldSpec(r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getBoolean(5)));}}
        if(spec.fields.isEmpty()) {
            String fallback="SELECT f.field_name,f.logical_type,f.physical_type,f.semantic_role,f.required FROM platform.contract_field_definition f JOIN platform.contract_table_definition t ON t.table_definition_id=f.table_definition_id WHERE t.logical_table_code=? ORDER BY t.revision DESC,f.ordinal";
            Set<String> existing=spec.fields.stream().map(FieldSpec::name).map(x->x.toLowerCase(Locale.ROOT)).collect(java.util.stream.Collectors.toSet());
            try(PreparedStatement p=c.prepareStatement(fallback)){p.setString(1,spec.logicalCode);try(ResultSet r=p.executeQuery()){while(r.next()){String n=r.getString(1);if(existing.add(n.toLowerCase(Locale.ROOT)))spec.fields.add(new FieldSpec(n,r.getString(2),r.getString(3),r.getString(4),r.getBoolean(5)));}}}
        }
        if(spec.fields.isEmpty()) throw new IllegalStateException("Contract table has no field definitions: "+spec.logicalCode+" revision="+revision);
    }

    private static void createTable(Database db,TableSpec spec)throws Exception {
        if(spec.accessName==null||spec.accessName.isBlank()) throw new IllegalStateException("Missing Access table name: "+spec.logicalCode);
        TableBuilder b=new TableBuilder(spec.accessName);
        Set<String> seen=new HashSet<>(); for(FieldSpec f:spec.fields){if(!seen.add(f.name.toLowerCase(Locale.ROOT)))throw new IllegalStateException("Duplicate field "+f.name+" in "+spec.logicalCode); b.addColumn(new ColumnBuilder(f.name,toAccessType(f.physicalType,f.logicalType)));}
        b.toTable(db);
        String[] pk=splitKey(spec.primaryKey); if(pk.length>0 && Arrays.stream(pk).allMatch(seen::contains)) new IndexBuilder(IndexBuilder.PRIMARY_KEY_NAME).addColumns(pk).setPrimaryKey().addToTable(db.getTable(spec.accessName));
    }

    private static String[] splitKey(String key){if(key==null||key.isBlank()||key.equalsIgnoreCase("contract-declared key"))return new String[0];return Arrays.stream(key.split(",")).map(String::trim).filter(s->!s.isBlank()).toArray(String[]::new);}
    private static DataType toAccessType(String physical,String logical){String t=(physical==null?logical:physical).toUpperCase(Locale.ROOT);return switch(t){case "BOOLEAN","BIT"->DataType.BOOLEAN;case "LONG","INTEGER","INT"->DataType.LONG;case "DOUBLE","DECIMAL","NUMERIC","FLOAT"->DataType.DOUBLE;case "SHORT_DATE_TIME","DATE","DATETIME2","TIMESTAMP"->DataType.SHORT_DATE_TIME;case "MEMO","LONGVARCHAR","NVARCHAR_MAX"->DataType.MEMO;default->DataType.TEXT;};}
    private record FieldSpec(String name,String logicalType,String physicalType,String semanticRole,boolean required){}
    private record TableSpec(long id,String logicalCode,String accessName,String primaryKey,String family,String grain,List<FieldSpec> fields){}
    private record RelationSpec(String primaryTable,String primaryField,String foreignTable,String foreignField,String name,String cardinality,boolean required){}
    private record IndexSpec(String tableName,String code,String kind,String fieldList,boolean unique){}
}
