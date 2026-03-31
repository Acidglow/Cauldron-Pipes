package net.minecraft.client.multiplayer.chat.report;

import com.mojang.authlib.minecraft.report.AbuseReport;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.authlib.minecraft.report.ReportedEntity;
import com.mojang.datafixers.util.Either;
import java.time.Instant;
import java.util.UUID;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.reporting.NameReportScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class NameReport extends Report {
    private final String reportedName;

    NameReport(UUID reportId, Instant createdAt, UUID reportedProfileId, String reportedName) {
        super(reportId, createdAt, reportedProfileId);
        this.reportedName = reportedName;
    }

    public String getReportedName() {
        return this.reportedName;
    }

    public NameReport copy() {
        NameReport namereport = new NameReport(this.reportId, this.createdAt, this.reportedProfileId, this.reportedName);
        namereport.comments = this.comments;
        namereport.attested = this.attested;
        return namereport;
    }

    @Override
    public Screen createScreen(Screen p_299843_, ReportingContext p_299844_) {
        return new NameReportScreen(p_299843_, p_299844_, this);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Builder extends Report.Builder<NameReport> {
        public Builder(NameReport report, AbuseReportLimits limits) {
            super(report, limits);
        }

        public Builder(UUID reportedProfileId, String reportedName, AbuseReportLimits limits) {
            super(new NameReport(UUID.randomUUID(), Instant.now(), reportedProfileId, reportedName), limits);
        }

        @Override
        public boolean hasContent() {
            return StringUtils.isNotEmpty(this.comments());
        }

        @Override
        public Report.@Nullable CannotBuildReason checkBuildable() {
            return this.report.comments.length() > this.limits.maxOpinionCommentsLength() ? Report.CannotBuildReason.COMMENT_TOO_LONG : super.checkBuildable();
        }

        @Override
        public Either<Report.Result, Report.CannotBuildReason> build(ReportingContext p_299891_) {
            Report.CannotBuildReason report$cannotbuildreason = this.checkBuildable();
            if (report$cannotbuildreason != null) {
                return Either.right(report$cannotbuildreason);
            } else {
                ReportedEntity reportedentity = new ReportedEntity(this.report.reportedProfileId);
                AbuseReport abusereport = AbuseReport.name(this.report.comments, reportedentity, this.report.createdAt);
                return Either.left(new Report.Result(this.report.reportId, ReportType.USERNAME, abusereport));
            }
        }
    }
}
