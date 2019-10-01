package dev.galasa.devtools.karaf;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Dictionary;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.annotations.ServiceScope;

import dev.galasa.framework.spi.FrameworkException;
import dev.galasa.framework.spi.IConfigurationPropertyStoreService;
import dev.galasa.framework.spi.IDynamicStatusStoreService;
import dev.galasa.framework.spi.IFramework;

@Component(immediate=true, 
service= {DevEnvironment.class},
scope=ServiceScope.SINGLETON)
public class DevEnvironment {

	@Reference(policy=ReferencePolicy.STATIC,
			policyOption=ReferencePolicyOption.GREEDY)
	private IFramework framework;

	private String namespace;
	private String runName;

	private IConfigurationPropertyStoreService cps;
	private IDynamicStatusStoreService         dss;

	public static final DateTimeFormatter DTF = DateTimeFormatter.RFC_1123_DATE_TIME;

	public DevEnvironment() {
	}

	@Activate
	public void activate() {
		System.out.println("DevTools Environment service activated");
		System.out.println("DevTools version  = " + getBundleVersion(getClass()));
		System.out.println("DevTools build    = " + getBundleBuild(getClass()));
		System.out.println("Framework version = " + getBundleVersion(IFramework.class));
		System.out.println("Framework build   = " + getBundleBuild(IFramework.class));
		if (this.framework.isInitialised()) {
			System.out.println("Framework is initialised");
		} else {
			System.out.println("Framework is not initialised");
		}

	}

	@Deactivate
	public void deactivate() {
		System.out.println("DevTools Environment service deactivated");
	}
	
	public static DevEnvironment getDevEnvironment() {
		BundleContext context = FrameworkUtil.getBundle(DevEnvironment.class).getBundleContext();
		ServiceReference<DevEnvironment> sr = context.getServiceReference(DevEnvironment.class);
		if (sr == null) {
			return null;
		}

		return context.getService(sr);
	}

	public boolean isFrameworkInitialised() {
		return framework.isInitialised();
	}

	public IFramework getFramework() {
		return this.framework;
	}

	public String getNamespace() {
		return this.namespace;
	}

	public void setNamespace(String namespace) throws FrameworkException {
		this.namespace = namespace;

		this.cps = this.framework.getConfigurationPropertyService(this.namespace);
		this.dss = this.framework.getDynamicStatusStoreService(this.namespace);
	}

	public IDynamicStatusStoreService getDSS() {
		return this.dss;
	}

	public IConfigurationPropertyStoreService getCPS() {
		return this.cps;
	}

	private String getBundleVersion(Class<?> klass) {
		String version = "UNKNOWN";

		Dictionary<String, String> headers = FrameworkUtil.getBundle(klass).getHeaders();
		if (headers != null) {
			String bundleVersion = headers.get("Bundle-Version");
			if (bundleVersion != null) {
				version = bundleVersion;
			}
		}

		return version;
	}

	private String getBundleBuild(Class<?> klass) {
		String build = "UNKNOWN";
		Dictionary<String, String> headers = FrameworkUtil.getBundle(klass).getHeaders();
		if (headers != null) {
			String bndLastModified = headers.get("Bnd-LastModified");
			if (bndLastModified != null) {
				Instant time = Instant.ofEpochMilli(Long.parseLong(bndLastModified));
				ZonedDateTime zdt = ZonedDateTime.ofInstant(time, ZoneId.systemDefault());
				build = zdt.format(DTF);
			}
		}

		return build;
	}

	public void setRunName(String runName) {
		this.runName = runName;
	}

	public String getRunName() {
		return this.runName;
	}

}
