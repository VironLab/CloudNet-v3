package de.dytanic.cloudnet.ext.syncproxy;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncProxyProxyLoginConfiguration {

  protected String targetGroup;

  protected boolean maintenance;

  protected int maxPlayers;

  protected List<String> whitelist;

  protected List<SyncProxyMotd> motds;

  protected List<SyncProxyMotd> maintenanceMotds;

}