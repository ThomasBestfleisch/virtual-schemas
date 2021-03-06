package com.exasol.adapter.dialects.impl;

import com.exasol.adapter.capabilities.Capabilities;
import com.exasol.adapter.dialects.AbstractSqlDialect;
import com.exasol.adapter.dialects.SqlDialectContext;
import com.exasol.adapter.dialects.SqlGenerationContext;
import com.exasol.adapter.metadata.DataType;
import com.exasol.adapter.sql.ScalarFunction;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * This class is work-in-progress
 *
 * TODO The precision of interval type columns is hardcoded, because it cannot be retrieved via JDBC. Should be retrieved from system table.<br>
 * TODO The srid of geometry type columns is hardcoded, because it cannot be retrieved via JDBC. Should be retrieved from system table.<br>
 */
public class ExasolSqlDialect extends AbstractSqlDialect {

    public ExasolSqlDialect(SqlDialectContext context) {
        super(context);
        omitParenthesesMap.add(ScalarFunction.SYSDATE);
        omitParenthesesMap.add(ScalarFunction.SYSTIMESTAMP);
        omitParenthesesMap.add(ScalarFunction.CURRENT_SCHEMA);
        omitParenthesesMap.add(ScalarFunction.CURRENT_SESSION);
        omitParenthesesMap.add(ScalarFunction.CURRENT_STATEMENT);
        omitParenthesesMap.add(ScalarFunction.CURRENT_USER);
    }

    public static final String NAME = "EXASOL";

    public String getPublicName() {
        return NAME;
    }

    @Override
    public SchemaOrCatalogSupport supportsJdbcCatalogs() {
        return SchemaOrCatalogSupport.UNSUPPORTED;
    }

    @Override
    public SchemaOrCatalogSupport supportsJdbcSchemas() {
        return SchemaOrCatalogSupport.SUPPORTED;
    }

    @Override
    public DataType mapJdbcType(ResultSet cols) throws SQLException {
        DataType colType = null;
        int jdbcType = cols.getInt("DATA_TYPE");
        switch (jdbcType) {
            case -104:
                // Currently precision is hardcoded, because we cannot retrieve it via EXASOL jdbc driver.
                colType = DataType.createIntervalDaySecond(2,3);
                break;
            case -103:
                // Currently precision is hardcoded, because we cannot retrieve it via EXASOL jdbc driver.
                colType = DataType.createIntervalYearMonth(2);
                break;
            case 123:
                // Currently srid is hardcoded, because we cannot retrieve it via EXASOL jdbc driver.
                colType = DataType.createGeometry(3857);
                break;
            case 124:
                colType = DataType.createTimestamp(true);
                break;
        }
        return colType;
    }

    @Override
    public Capabilities getCapabilities() {
        // Supports all capabilities
        Capabilities cap = new Capabilities();
        cap.supportAllCapabilities();
        return cap;
    }

    @Override
    public IdentifierCaseHandling getUnquotedIdentifierHandling() {
        return IdentifierCaseHandling.INTERPRET_AS_UPPER;
    }

    @Override
    public IdentifierCaseHandling getQuotedIdentifierHandling() {
        return IdentifierCaseHandling.INTERPRET_CASE_SENSITIVE;
    }

    @Override
    public String applyQuote(String identifier) {
        // If identifier contains double quotation marks ", it needs to be espaced by another double quotation mark. E.g. "a""b" is the identifier a"b in the db.
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    @Override
    public String applyQuoteIfNeeded(String identifier) {
        // Quoted identifiers can contain any unicode char except dot (.).
        // This is a simplified rule, which might cause that some identifiers are quoted although not needed
        boolean isSimpleIdentifier = identifier.matches("^[A-Z][0-9A-Z_]*");
        if (isSimpleIdentifier) {
            return identifier;
        } else {
            return applyQuote(identifier);
        }
    }

    @Override
    public boolean requiresCatalogQualifiedTableNames(SqlGenerationContext context) {
        return false;
    }

    @Override
    public boolean requiresSchemaQualifiedTableNames(SqlGenerationContext context) {
        // We need schema qualifiers a) if we are in IS_LOCAL mode, i.e. we run statements directly in a subselect without IMPORT FROM JDBC
        // and b) if we don't have the schema in the jdbc connection string (like "jdbc:exa:localhost:5555;schema=native")
        return true;
        // return context.isLocal();
    }

    @Override
    public NullSorting getDefaultNullSorting() {
        assert(getContext().getSchemaAdapterNotes().isNullsAreSortedHigh());
        return NullSorting.NULLS_SORTED_HIGH;
    }

    @Override
    public String getStringLiteral(String value) {
        // Don't forget to escape single quote
        return "'" + value.replace("'", "''") + "'";
    }

}
