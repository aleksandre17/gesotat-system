import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Produces a new build of an Access package: a copy whose declared package version is changed and whose
 * business rows are untouched. The table and column are the ones every canonical package declares
 * ({@code __gs_package.package_version}); nothing here names a site or a dataset.
 *
 * Usage: AccessPackageVersionStamper <source.accdb> <target.accdb> <version>
 */
public final class AccessPackageVersionStamper {
    private static final String PACKAGE_TABLE = "__gs_package";
    private static final String VERSION_COLUMN = "package_version";

    public static void main(String[] args) throws Exception {
        if (args.length != 3 || !args[2].matches("[0-9]+(\\.[0-9]+){1,3}")) {
            System.err.println("usage: AccessPackageVersionStamper <source.accdb> <target.accdb> <version e.g. 8.0.1>");
            System.exit(64);
        }
        Path source = Path.of(args[0]).toAbsolutePath().normalize();
        Path target = Path.of(args[1]).toAbsolutePath().normalize();
        if (source.equals(target)) throw new IllegalArgumentException("The source package is never modified; choose another target");
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        try (Database database = new DatabaseBuilder(target.toFile()).open()) {
            Table packages = database.getTable(PACKAGE_TABLE);
            if (packages == null || packages.getRowCount() != 1)
                throw new IllegalStateException(PACKAGE_TABLE + " must contain exactly one package row");
            Row row = packages.iterator().next();
            Object previous = row.get(VERSION_COLUMN);
            row.put(VERSION_COLUMN, args[2]);
            packages.updateRow(row);
            System.out.println("package_version " + previous + " -> " + args[2] + " target=" + target);
        }
    }
}
